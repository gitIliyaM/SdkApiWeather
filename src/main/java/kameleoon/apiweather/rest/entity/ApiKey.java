package kameleoon.apiweather.rest.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @Column(name = "api_key", length = 64)
    private String apiKey;

    public ApiKey() {}

    public ApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}