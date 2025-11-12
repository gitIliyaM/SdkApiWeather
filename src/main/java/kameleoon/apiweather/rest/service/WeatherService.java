package kameleoon.apiweather.rest.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kameleoon.apiweather.rest.dto.WeatherResponseDto;
import kameleoon.apiweather.rest.exception.ApiKeyNotFoundException;
import kameleoon.apiweather.rest.exception.CityNotFoundException;
import kameleoon.apiweather.rest.exception.CustomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);
    private static final int CACHE_SIZE_LIMIT = 10;
    private static final long CACHE_VALIDITY_MINUTES = 1;
    private final ApiKeyService apiKeyService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private ScheduledExecutorService pollingScheduler;
    private volatile boolean pollingModeActive = false;

    @Value("${openweathermap.api.url}")
    private String weatherApiUrl;

    private final Map<String, CacheEntry> cityCache = Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                    return size() > CACHE_SIZE_LIMIT;
                }
            }
    );

    public WeatherService(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    private static class CacheEntry {
        final String data;
        final LocalDateTime timestamp;
        final String originalApiKey;

        CacheEntry(String data, LocalDateTime timestamp, String originalApiKey) {
            this.data = data;
            this.timestamp = timestamp;
            this.originalApiKey = originalApiKey;
        }
    }

    @PostConstruct
    public void init() {
        pollingScheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @PreDestroy
    public void shutdown() {
        if (pollingScheduler != null) {
            pollingScheduler.shutdown();
            try {
                if (!pollingScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    pollingScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                pollingScheduler.shutdownNow();
            }
        }
    }

    public WeatherResponseDto getWeatherForCity(String cityName, String apiKey, String mode) {
        if (!apiKeyService.isValidApiKey(apiKey)) {
            throw new ApiKeyNotFoundException("API Key not found in the database. Please save the key first.");
        }

        boolean isPollingRequest = "polling".equalsIgnoreCase(mode);
        if (isPollingRequest && !pollingModeActive) {
            synchronized (this) {
                if (!pollingModeActive) {
                    pollingModeActive = true;
                    startPollingTask();
                }
            }
        }

        CacheEntry cachedEntry;
        synchronized (cityCache) {
            cachedEntry = cityCache.get(cityName);
        }

        if (cachedEntry != null) {
            LocalDateTime now = LocalDateTime.now();
            long minutesSinceUpdate = ChronoUnit.MINUTES.between(cachedEntry.timestamp, now);
            if (minutesSinceUpdate < CACHE_VALIDITY_MINUTES) {
                try {
                    return objectMapper.readValue(cachedEntry.data, WeatherResponseDto.class);
                } catch (Exception e) {
                    logger.warn("Cached data for {} is corrupted: {}", cityName, e.getMessage());
                }
            } else {
                synchronized (cityCache) {
                    cityCache.remove(cityName);
                }
            }
        }

        // Запрос свежих данных
        String weatherData = fetchWeatherData(cityName, apiKey);
        CacheEntry newEntry = new CacheEntry(weatherData, LocalDateTime.now(), apiKey);

        synchronized (cityCache) {
            cityCache.put(cityName, newEntry);
        }

        try {
            return objectMapper.readValue(weatherData, WeatherResponseDto.class);
        } catch (Exception e) {
            throw new CustomException("Failed to parse weather data from API for city: " + cityName);
        }
    }

    private String fetchWeatherData(String cityName, String apiKey) {
        String url = String.format("%s?q=%s&appid=%s&units=metric", weatherApiUrl, cityName, apiKey);
        String response = restTemplate.getForObject(url, String.class);
        if (response == null) {
            throw new CustomException("API returned null response for city: " + cityName);
        }

        try {
            JsonNode responseNode = objectMapper.readTree(response);
            if (responseNode.has("cod") && responseNode.get("cod").asInt() != 200) {
                String message = responseNode.path("message").asText("Unknown API Error");
                if ("city not found".equalsIgnoreCase(message)) {
                    throw new CityNotFoundException("Weather data not found for city: " + cityName);
                } else {
                    throw new CustomException("OpenWeatherMap API error: " + message);
                }
            }
            return response;
        } catch (Exception e) {
            throw new CustomException("Failed to parse API response for city " + cityName + ": " + e.getMessage());
        }
    }

    private void startPollingTask() {
        pollingScheduler.scheduleWithFixedDelay(() -> {
            LocalDateTime now = LocalDateTime.now();
            List<Map.Entry<String, String>> citiesAndKeys = new ArrayList<>();

            // 🔒 Безопасное чтение всего кэша
            synchronized (cityCache) {
                for (Map.Entry<String, CacheEntry> entry : cityCache.entrySet()) {
                    String city = entry.getKey();
                    CacheEntry cacheEntry = entry.getValue();
                    if (cacheEntry != null) {
                        long minutesSinceUpdate = ChronoUnit.MINUTES.between(cacheEntry.timestamp, now);
                        if (minutesSinceUpdate >= CACHE_VALIDITY_MINUTES) {
                            citiesAndKeys.add(new AbstractMap.SimpleEntry<>(city, cacheEntry.originalApiKey));
                        }
                    }
                }
            }

            for (Map.Entry<String, String> item : citiesAndKeys) {
                String cityName = item.getKey();
                String apiKey = item.getValue();
                try {
                    String updatedData = fetchWeatherData(cityName, apiKey);
                    CacheEntry updatedEntry = new CacheEntry(updatedData, LocalDateTime.now(), apiKey);

                    synchronized (cityCache) {
                        cityCache.put(cityName, updatedEntry);
                    }

                    logger.info("Polling: updated weather for city {}", cityName);
                } catch (Exception e) {
                    logger.warn("Polling update failed for city {}: {}", cityName, e.getMessage());
                }
            }
        }, 0, 1, TimeUnit.MINUTES);
    }

    public List<String> getCachedCities() {
        synchronized (cityCache) {
            return new ArrayList<>(cityCache.keySet());
        }
    }
}