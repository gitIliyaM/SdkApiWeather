package kameleoon.apiweather.sdk.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kameleoon.apiweather.sdk.SdkWeather;
import kameleoon.apiweather.sdk.dto.WeatherResponseSdkDto;
import kameleoon.apiweather.sdk.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class SdkWeatherService {

    private static final Logger logger = LoggerFactory.getLogger(SdkWeatherService.class);
    private static final int CACHE_SIZE_LIMIT = 10;
    private static final long CACHE_VALIDITY_MINUTES = 1;

    @Value("${openweathermap.api.url}")
    private String weatherApiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, Map<String, CacheEntry>> apiKeyCache = new ConcurrentHashMap<>();

    private final Map<String, ScheduledExecutorService> pollingSchedulers = new ConcurrentHashMap<>();

    private final Map<String, Boolean> pollingActiveFlags = new ConcurrentHashMap<>();

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

    @PreDestroy
    public void shutdown() {
        for (ScheduledExecutorService scheduler : pollingSchedulers.values()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
        }
    }

    public Map<String, Object> getSDKCacheStatsSuccess(String apiKey, Map<String, Object> stats) {
        return Map.of(
                "status", "success",
                "apiKey", apiKey,
                "cacheStats", stats
        );
    }

    public Map<String, Object> getSDKClearCacheSuccess() {
        return Map.of(
                "status", "success",
                "message", "Cache cleared successfully"
        );
    }

    public Map<String, Object> getSDKInfoSuccess(String apiKey, String mode) {
        return Map.of(
                "status", "success",
                "apiKey", apiKey,
                "mode", mode
        );
    }

    public  Map<String, Object> getSDKCachedCitiesCityNameSuccess(String cityName, Map<String, Object> cityInfo){
        return Map.of(
                "status", "success",
                "cityName", cityName,
                "data", cityInfo
        );
    }

    public  Map<String, Object> getSDKCachedCitiesSuccess(String apiKey, List<String> cities){
        return Map.of(
                "status", "success",
                "apiKey", apiKey,
                "cachedCities", cities,
                "totalCities", cities.size()
        );
    }

    public  Map<String, Object> getSDKStatusResultsSuccess(List<Map<String, Object>> results){
        return Map.of(
                "status", "success",
                "results", results);
    }

    public  Map<String, Object> getSDKInstanceRemovedSuccess(){
        return Map.of(
                "status", "success",
                "message", "SDK instance removed successfully"
        );
    }

    public  Map<String, Object> getSDKInitializedSuccess(String mode){
        return Map.of(
                "status", "success",
                "message", "SDK initialized successfully",
                "mode", mode);
    }

    public List<Map<String, Object>> getMultipleWeatherForCities(String apiKey, List<String> cities) {
        if (cities == null || cities.isEmpty()) {
            throw new SdkCitiesListRequiredException("City list is required.");
        }

        SdkWeather sdk = SdkWeather.getInstance(apiKey);

        return cities.stream()
                .map(city -> {
                    try {
                        WeatherResponseSdkDto weather = sdk.getWeather(city);
                        return Map.of("city", city, "data", weather);
                    } catch (Exception e) {
                        return Map.<String, Object>of("city", city, "error", "Failed to process weather: " + e.getMessage());
                    }
                })
                .collect(Collectors.toList());
    }

    public WeatherResponseSdkDto getWeatherForCity(String cityName, String apiKey, String mode) {
        boolean isPollingRequest = "polling".equalsIgnoreCase(mode);
        if (isPollingRequest) {
            ensurePollingStarted(apiKey);
        }

        Map<String, CacheEntry> cityCache = apiKeyCache.computeIfAbsent(apiKey, k -> new ConcurrentHashMap<>());

        CacheEntry cachedEntry = cityCache.get(cityName);
        if (cachedEntry != null) {
            LocalDateTime now = LocalDateTime.now();
            long minutesSinceUpdate = ChronoUnit.MINUTES.between(cachedEntry.timestamp, now);
            if (minutesSinceUpdate < CACHE_VALIDITY_MINUTES) {
                try {
                    return objectMapper.readValue(cachedEntry.data, WeatherResponseSdkDto.class);
                } catch (Exception e) {
                    logger.warn("Cached data for {} (apiKey {}) is corrupted: {}", cityName, apiKey, e.getMessage());
                }
            } else {
                cityCache.remove(cityName);
            }
        }

        String weatherData = fetchWeatherData(cityName, apiKey);
        cityCache.put(cityName, new CacheEntry(weatherData, LocalDateTime.now(), apiKey));

        try {
            return objectMapper.readValue(weatherData, WeatherResponseSdkDto.class);
        } catch (Exception e) {
            throw new SdkCustomException("Failed to parse weather data from API for city: " + cityName);
        }
    }

    private void ensurePollingStarted(String apiKey) {
        if (!pollingActiveFlags.getOrDefault(apiKey, false)) {
            synchronized (this) {
                if (!pollingActiveFlags.getOrDefault(apiKey, false)) {
                    pollingActiveFlags.put(apiKey, true);
                    startPollingTaskForApiKey(apiKey);
                    logger.info("Polling started for apiKey: {}", apiKey);
                }
            }
        }
    }

    private void startPollingTaskForApiKey(String apiKey) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(() -> {
            logger.debug("Polling task running for apiKey: {}", apiKey);

            Map<String, CacheEntry> cityCache = apiKeyCache.get(apiKey);
            if (cityCache == null || cityCache.isEmpty()) {
                logger.debug("No cached cities for apiKey: {}", apiKey);
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            List<String> citiesToRefresh = new ArrayList<>();
            for (String cityName : cityCache.keySet()) {
                CacheEntry entry = cityCache.get(cityName);
                if (entry != null) {
                    long minutesSinceUpdate = ChronoUnit.MINUTES.between(entry.timestamp, now);
                    if (minutesSinceUpdate >= CACHE_VALIDITY_MINUTES) {
                        citiesToRefresh.add(cityName);
                    }
                }
            }

            for (String cityName : citiesToRefresh) {
                try {
                    CacheEntry entry = cityCache.get(cityName);
                    if (entry != null) {
                        String updatedData = fetchWeatherData(cityName, entry.originalApiKey);
                        cityCache.put(cityName, new CacheEntry(updatedData, LocalDateTime.now(), entry.originalApiKey));
                        logger.info("Polling: updated weather for city {} (apiKey {})", cityName, apiKey);
                    }
                } catch (Exception e) {
                    logger.warn("Polling update failed for city {} (apiKey {}): {}", cityName, apiKey, e.getMessage());
                }
            }
        }, 0, 1, TimeUnit.MINUTES); // Обновление каждую минуту

        pollingSchedulers.put(apiKey, scheduler);
    }

    private String fetchWeatherData(String cityName, String apiKey) {
        String url = String.format("%s?q=%s&appid=%s&units=metric", weatherApiUrl, cityName, apiKey);

        String response = restTemplate.getForObject(url, String.class);
        if (response == null) {
            throw new SdkCustomException("API returned null response for city: " + cityName);
        }

        try {
            JsonNode responseNode = objectMapper.readTree(response);
            if (responseNode.has("cod") && responseNode.get("cod").asInt() != 200) {
                String message = responseNode.path("message").asText("Unknown API Error");
                if ("city not found".equalsIgnoreCase(message)) {
                    throw new SdkCityNotFoundException("Weather data not found for city: " + cityName);
                } else {
                    throw new SdkCustomException("OpenWeatherMap API error: " + message);
                }
            }
            return response;
        } catch (JsonProcessingException e) {
            throw new SdkCustomException("Failed to parse API response for city " + cityName + ": " + e.getMessage());
        }
    }

    public List<String> getCachedCities(String apiKey) {
        Map<String, CacheEntry> cityCache = apiKeyCache.get(apiKey);
        if (cityCache == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(cityCache.keySet());
    }

    public Map<String, Object> getCachedCityInfo(String apiKey, String cityName) {
        Map<String, CacheEntry> cityCache = apiKeyCache.get(apiKey);
        if (cityCache == null) {
            throw new SdkApiKeyNotFoundException("No cache found for API key: " + apiKey);
        }
        CacheEntry entry = cityCache.get(cityName);
        if (entry == null) {
            throw new SdkCityNotInCacheException("City '" + cityName + "' not found in cache for API key: " + apiKey);
        }
        Map<String, Object> info = new ConcurrentHashMap<>();
        info.put("cityName", cityName);
        info.put("timestamp", entry.timestamp);
        info.put("ageMinutes", ChronoUnit.MINUTES.between(entry.timestamp, LocalDateTime.now()));
        info.put("data", entry.data);
        return info;
    }

    public Map<String, Object> getCacheStats(String apiKey) {
        Map<String, CacheEntry> cityCache = apiKeyCache.get(apiKey);
        int size = cityCache != null ? cityCache.size() : 0;

        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("size", size);
        stats.put("maxSize", CACHE_SIZE_LIMIT);
        stats.put("apiKey", apiKey);
        stats.put("pollingActive", pollingActiveFlags.getOrDefault(apiKey, false));
        return stats;
    }

    public void clearCacheForApiKey(String apiKey) {
        Map<String, CacheEntry> cityCache = apiKeyCache.get(apiKey);
        if (cityCache != null) {
            cityCache.clear();
        }
    }

    public void removeCacheForApiKey(String apiKey) {
        apiKeyCache.remove(apiKey);
        ScheduledExecutorService scheduler = pollingSchedulers.remove(apiKey);
        if (scheduler != null) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
        }
        pollingActiveFlags.remove(apiKey);
        logger.info("Cache and polling removed for apiKey: {}", apiKey);
    }
}