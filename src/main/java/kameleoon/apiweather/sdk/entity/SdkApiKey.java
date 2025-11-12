package kameleoon.apiweather.sdk.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "api_keys")
public class SdkApiKey {

    @Id
    @Column(name = "api_key", length = 64)
    private String apiKey;

    public SdkApiKey() {}

    public SdkApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}