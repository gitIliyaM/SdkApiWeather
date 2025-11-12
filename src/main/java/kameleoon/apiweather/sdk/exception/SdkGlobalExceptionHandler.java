package kameleoon.apiweather.sdk.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import java.util.stream.Collectors;

@ControllerAdvice
public class SdkGlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(SdkGlobalExceptionHandler.class);

    @ExceptionHandler(SdkCustomException.class)
    public ResponseEntity<String> handleCustomException(SdkCustomException e) {
        logger.error("Custom Exception: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(SdkApiKeyNotFoundException.class)
    public ResponseEntity<String> handleApiKeyNotFoundException(SdkApiKeyNotFoundException e) {
        logger.error("API Key Not Found Exception: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(SdkCityNotFoundException.class)
    public ResponseEntity<String> handleCityNotFoundException(SdkCityNotFoundException e) {
        logger.error("City Not Found Exception: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Invalid argument: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalStateException(IllegalStateException e) {
        logger.warn("Illegal state: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolation(ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        logger.warn("Validation constraint violated: {}", errorMessage);
        return ResponseEntity.badRequest().body(errorMessage);
    }

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<String> handleHttpClientError(HttpClientErrorException e) {
        logger.error("HTTP Client Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        return ResponseEntity.status(e.getStatusCode()).body("Client error: " + e.getResponseBodyAsString());
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<String> handleHttpServerError(HttpServerErrorException e) {
        logger.error("HTTP Server Error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        return ResponseEntity.status(e.getStatusCode()).body("Server error: " + e.getResponseBodyAsString());
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<String> handleRestClientException(RestClientException e) {
        logger.error("Rest client error", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body("Weather API is unavailable.");
    }

    @ExceptionHandler(com.fasterxml.jackson.core.JsonProcessingException.class)
    public ResponseEntity<String> handleJsonError(com.fasterxml.jackson.core.JsonProcessingException e) {
        logger.error("JSON parsing failed", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to parse weather data.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGenericException(Exception e) {
        logger.error("Generic Exception: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
    }

    @ExceptionHandler(SdkCitiesListRequiredException.class)
    public ResponseEntity<String> handleSdkCitiesListRequiredException(SdkCitiesListRequiredException e) {
        logger.warn("Cities list required error: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(SdkCityNotInCacheException.class)
    public ResponseEntity<String> handleSdkCityNotInCacheException(SdkCityNotInCacheException e) {
        logger.warn("City not in cache error: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}