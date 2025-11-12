package kameleoon.apiweather.rest.service;

import kameleoon.apiweather.rest.entity.ApiKey;
import kameleoon.apiweather.rest.exception.ApiKeyNotFoundException;
import kameleoon.apiweather.rest.repository.ApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyService {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyService.class);

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public String saveApiKey(String apiKey) {
        if (apiKeyRepository.existsByApiKey(apiKey)) {
            logger.warn("Attempt to save an existing API key: {}", apiKey);
            return "API Key already exists in the database.";
        }
        ApiKey newKey = new ApiKey(apiKey);
        apiKeyRepository.save(newKey);
        logger.info("API Key saved successfully: {}", apiKey);
        return "API Key saved successfully.";
    }

    public String deleteApiKey(String apiKey) {
        if (!apiKeyRepository.existsByApiKey(apiKey)) {
            logger.warn("Attempt to delete a non-existent API key: {}", apiKey);
            throw new ApiKeyNotFoundException("API Key not found in the database.");
        }
        apiKeyRepository.deleteById(apiKey);
        logger.info("API Key deleted successfully: {}", apiKey);
        return "API Key deleted successfully.";
    }

    public String getApiKey(String apiKey) {
        if (!apiKeyRepository.existsByApiKey(apiKey)) {
            logger.warn("Attempt to retrieve a non-existent API key: {}", apiKey);
            throw new ApiKeyNotFoundException("API Key not found in the database.");
        }
        logger.info("API Key retrieved successfully: {}", apiKey);
        return apiKey;
    }

    public boolean isValidApiKey(String apiKey) {
        return apiKeyRepository.existsByApiKey(apiKey);
    }
}