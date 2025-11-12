package kameleoon.apiweather.sdk;

import kameleoon.apiweather.sdk.dto.WeatherResponseSdkDto;
import kameleoon.apiweather.sdk.service.SdkWeatherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ConcurrentHashMap;

public class SdkWeather {

    private static final Logger logger = LoggerFactory.getLogger(SdkWeather.class);
    private static final ConcurrentHashMap<String, SdkWeather> instances = new ConcurrentHashMap<>();

    private final String apiKey;
    private final String mode;
    private final SdkWeatherService weatherServiceSdk;

    private SdkWeather(String apiKey, String mode, SdkWeatherService weatherServiceSdk) {
        this.apiKey = apiKey;
        this.mode = mode;
        this.weatherServiceSdk = weatherServiceSdk;
        logger.info("SdkWeather instance created for API key: {}, mode: {}", apiKey, mode);
    }

    public static SdkWeather createInstance(String apiKey, String mode, SdkWeatherService weatherServiceSdk) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("API key cannot be null or empty");
        }
        String normalizedKey = apiKey.trim();

        if (instances.containsKey(normalizedKey)) {
            throw new IllegalStateException("SDK instance with this API key already exists");
        }
        SdkWeather newInstance = new SdkWeather(normalizedKey, mode, weatherServiceSdk);
        instances.put(normalizedKey, newInstance);
        return newInstance;
    }

    public static SdkWeather getInstance(String apiKey) {
        if (apiKey == null) {
            throw new IllegalArgumentException("API key cannot be null");
        }
        SdkWeather instance = instances.get(apiKey.trim());
        if (instance == null) {
            throw new IllegalStateException("SDK instance not found for this API key. Please initialize first.");
        }
        return instance;
    }

    public WeatherResponseSdkDto getWeather(String cityName) {
        if (cityName == null || cityName.trim().isEmpty()) {
            throw new IllegalArgumentException("City name cannot be null or empty");
        }

        return weatherServiceSdk.getWeatherForCity(cityName, this.apiKey, this.mode);
    }

    public static void removeInstance(String apiKey) {
        if (apiKey == null) {
            throw new IllegalArgumentException("API key cannot be null");
        }
        SdkWeather removed = instances.remove(apiKey.trim());
        if (removed == null) {
            throw new IllegalStateException("SDK instance not found for this API key. Cannot remove.");
        }

        removed.weatherServiceSdk.removeCacheForApiKey(removed.apiKey);
        logger.info("SdkWeather instance removed for API key: {}", apiKey);
    }

    public boolean isInstanceExists(String apiKey) {
        return apiKey != null && instances.containsKey(apiKey.trim());
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getMode() {
        return mode;
    }
}