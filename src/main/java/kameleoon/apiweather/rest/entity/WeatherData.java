// Этот класс можно использовать для хранения кэшированных данных в БД,
// но в текущей реализации кэш хранится в памяти Map.
// Оставляю для потенциального расширения
package kameleoon.apiweather.rest.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cached_weather")
public class WeatherData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "city_name", nullable = false)
    private String cityName;

    @Lob
    @Column(name = "weather_json", columnDefinition = "jsonb")
    private String weatherJson;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    public WeatherData() {}

    public WeatherData(String cityName, String weatherJson, LocalDateTime timestamp) {
        this.cityName = cityName;
        this.weatherJson = weatherJson;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCityName() { return cityName; }
    public void setCityName(String cityName) { this.cityName = cityName; }

    public String getWeatherJson() { return weatherJson; }
    public void setWeatherJson(String weatherJson) { this.weatherJson = weatherJson; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}