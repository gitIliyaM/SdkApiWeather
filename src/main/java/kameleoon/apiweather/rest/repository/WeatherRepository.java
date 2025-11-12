// если хранить кэш в БД
/*package kameleoon.apiweather.rest.repository;

 import kameleoon.apiweather.rest.entity.WeatherData;
 import org.springframework.data.jpa.repository.JpaRepository;
 import org.springframework.stereotype.Repository;
 import java.util.Optional;

 @Repository
 public interface WeatherRepository extends JpaRepository<WeatherData, Long> {
     Optional<WeatherData> findByCityName(String cityName);
 }*/