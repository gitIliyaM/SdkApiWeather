package kameleoon.apiweather.sdk.controller;

import kameleoon.apiweather.sdk.SdkWeather;
import kameleoon.apiweather.sdk.dto.WeatherResponseSdkDto;
import kameleoon.apiweather.sdk.service.SdkApiKeyService;
import kameleoon.apiweather.sdk.service.SdkWeatherService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sdk")
@Validated
public class SdkController {

    private final SdkWeatherService weatherServiceSdk;
    private final SdkApiKeyService apiKeyService;

    public SdkController(SdkWeatherService weatherServiceSdk, SdkApiKeyService apiKeyService) {
        this.weatherServiceSdk = weatherServiceSdk;
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initializeSdk(
            @RequestParam @NotBlank String apiKey,
            @RequestParam(defaultValue = "on-demand") @Pattern(regexp = "^(on-demand|polling)$", message = "Mode must be 'on-demand' or 'polling'")
            String mode) {
        apiKeyService.getApiKey(apiKey);
        SdkWeather.createInstance(apiKey, mode, weatherServiceSdk);
        Map<String, Object> response = weatherServiceSdk.getSDKInitializedSuccess(mode);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{apiKey}")
    public ResponseEntity<Map<String, Object>> removeSdkInstance(@PathVariable @NotBlank String apiKey) {
        SdkWeather.removeInstance(apiKey);
        Map<String, Object> response = weatherServiceSdk.getSDKInstanceRemovedSuccess();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{apiKey}/weather/{cityName}")
    public ResponseEntity<WeatherResponseSdkDto> getWeather(
            @PathVariable @NotBlank String apiKey,
            @PathVariable @NotBlank String cityName) {
        SdkWeather sdk = SdkWeather.getInstance(apiKey);
        WeatherResponseSdkDto weather = sdk.getWeather(cityName);
        return ResponseEntity.ok(weather);
    }

    @PostMapping("/multiple")
    public ResponseEntity<Map<String, Object>> getMultipleCitiesWeather(
            @RequestParam @NotBlank String apiKey,
            @RequestBody Map<String, List<String>> requestBody) {
        List<String> cities = requestBody.get("cities");
        List<Map<String, Object>> results = weatherServiceSdk.getMultipleWeatherForCities(apiKey, cities);
        Map<String, Object> response = weatherServiceSdk.getSDKStatusResultsSuccess(results);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{apiKey}/cached-cities")
    public ResponseEntity<Map<String, Object>> getCachedCities(@PathVariable @NotBlank String apiKey) {
        List<String> cities = weatherServiceSdk.getCachedCities(apiKey);
        Map<String, Object> response = weatherServiceSdk.getSDKCachedCitiesSuccess(apiKey, cities);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{apiKey}/cached-cities/{cityName}")
    public ResponseEntity<Map<String, Object>> getCachedCityInfo(
            @PathVariable @NotBlank String apiKey,
            @PathVariable @NotBlank String cityName) {
        Map<String, Object> cityInfo = weatherServiceSdk.getCachedCityInfo(apiKey, cityName);
        Map<String, Object> response = weatherServiceSdk.getSDKCachedCitiesCityNameSuccess(apiKey, cityInfo);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{apiKey}/cache-stats")
    public ResponseEntity<Map<String, Object>> getCacheStats(@PathVariable @NotBlank String apiKey) {
        Map<String, Object> stats = weatherServiceSdk.getCacheStats(apiKey);
        Map<String, Object> response = weatherServiceSdk.getSDKCacheStatsSuccess(apiKey, stats);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{apiKey}/clear-cache")
    public ResponseEntity<Map<String, Object>> clearCache(@PathVariable @NotBlank String apiKey) {
        weatherServiceSdk.clearCacheForApiKey(apiKey);
        Map<String, Object> response = weatherServiceSdk.getSDKClearCacheSuccess();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{apiKey}/info")
    public ResponseEntity<Map<String, Object>> getSdkInfo(@PathVariable @NotBlank String apiKey) {
        SdkWeather sdk = SdkWeather.getInstance(apiKey);
        Map<String, Object> response = weatherServiceSdk.getSDKInfoSuccess(sdk.getApiKey(), sdk.getMode());
        return ResponseEntity.ok(response);
    }
}