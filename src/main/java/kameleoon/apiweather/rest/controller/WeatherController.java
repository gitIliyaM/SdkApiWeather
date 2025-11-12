package kameleoon.apiweather.rest.controller;

import kameleoon.apiweather.rest.dto.WeatherResponseDto;
import kameleoon.apiweather.rest.service.ApiKeyService;
import kameleoon.apiweather.rest.service.WeatherService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Validated
public class WeatherController {

    private final WeatherService weatherService;
    private final ApiKeyService apiKeyService;

    public WeatherController(WeatherService weatherService, ApiKeyService apiKeyService) {
        this.weatherService = weatherService;
        this.apiKeyService = apiKeyService;
    }

    @PostMapping("/save/{apiKey}")
    public ResponseEntity<String> saveApiKey(
            @PathVariable @NotBlank String apiKey) {
        return ResponseEntity.ok(apiKeyService.saveApiKey(apiKey));
    }

    @DeleteMapping("/delete/{apiKey}")
    public ResponseEntity<String> deleteApiKey(
            @PathVariable @NotBlank String apiKey) {
        return ResponseEntity.ok(apiKeyService.deleteApiKey(apiKey));
    }

    @GetMapping("/get/{apiKey}")
    public ResponseEntity<String> getApiKey(
            @PathVariable @NotBlank String apiKey) {
        return ResponseEntity.ok(apiKeyService.getApiKey(apiKey));
    }

    @GetMapping("/{cityName}/{apiKey}/{mode}")
    public ResponseEntity<WeatherResponseDto> getWeatherWithMode(
            @PathVariable @NotBlank String cityName,
            @PathVariable @NotBlank String apiKey,
            @PathVariable @Pattern(regexp = "^(on-demand|polling)$", message = "Invalid mode. Use 'on-demand' or 'polling'.")
            String mode) {
        return ResponseEntity.ok(weatherService.getWeatherForCity(cityName, apiKey, mode));
    }

    @GetMapping("/{cityName}/{apiKey}")
    public ResponseEntity<WeatherResponseDto> getWeather(
            @PathVariable @NotBlank String cityName,
            @PathVariable @NotBlank String apiKey) {
        return ResponseEntity.ok(weatherService.getWeatherForCity(cityName, apiKey, "on-demand"));
    }

    @GetMapping("/cached-cities")
    public ResponseEntity<java.util.List<String>> getCachedCities() {
        return ResponseEntity.ok(weatherService.getCachedCities());
    }

    @GetMapping("/{cityName}")
    public ResponseEntity<String> handleMissingApiKey(@PathVariable String cityName) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("API key is required.");
    }

    @GetMapping("//{apiKey}")
    public ResponseEntity<String> handleMissingCity(@PathVariable String apiKey) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("City name is required.");
    }

    @GetMapping("//")
    public ResponseEntity<String> handleMissingCityApiKey() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("City name and API key is required.");
    }
}