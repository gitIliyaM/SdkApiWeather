package kameleoon.apiweather.sdk.service;

import kameleoon.apiweather.sdk.entity.SdkApiKey;
import kameleoon.apiweather.sdk.exception.SdkApiKeyNotFoundException;
import kameleoon.apiweather.sdk.repository.SdkApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SdkApiKeyService {

    private static final Logger logger = LoggerFactory.getLogger(SdkApiKeyService.class);
    private final SdkApiKeyRepository apiKeyRepository;

    public SdkApiKeyService(SdkApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    public String saveApiKey(String apiKey) {
        if (apiKeyRepository.existsByApiKey(apiKey)) {
            logger.warn("Attempt to save an existing API key: {}", apiKey);
            return "API Key already exists in the database.";
        }
        SdkApiKey newKey = new SdkApiKey(apiKey);
        apiKeyRepository.save(newKey);
        logger.info("API Key saved successfully: {}", apiKey);
        return "API Key saved successfully.";
    }

    public String deleteApiKey(String apiKey) {
        if (!apiKeyRepository.existsByApiKey(apiKey)) {
            logger.warn("Attempt to delete a non-existent API key: {}", apiKey);
            throw new SdkApiKeyNotFoundException("API Key not found in the database.");
        }
        apiKeyRepository.deleteById(apiKey);
        logger.info("API Key deleted successfully: {}", apiKey);
        return "API Key deleted successfully.";
    }

    public void getApiKey(String apiKey) {
        if (!apiKeyRepository.existsByApiKey(apiKey)) {
            logger.warn("Attempt to retrieve a non-existent API key: {}", apiKey);
            throw new SdkApiKeyNotFoundException("API Key not found in the database.");
        }
        logger.info("API Key retrieved successfully: {}", apiKey);
    }

    public boolean isValidApiKey(String apiKey) {
        return apiKeyRepository.existsByApiKey(apiKey);
    }
}