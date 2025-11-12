package kameleoon.apiweather.sdk.repository;

import kameleoon.apiweather.sdk.entity.SdkApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SdkApiKeyRepository extends JpaRepository<SdkApiKey, String> {
    boolean existsByApiKey(String apiKey);
}