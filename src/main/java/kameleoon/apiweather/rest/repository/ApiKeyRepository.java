package kameleoon.apiweather.rest.repository;

import kameleoon.apiweather.rest.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, String> {
    boolean existsByApiKey(String apiKey);
}