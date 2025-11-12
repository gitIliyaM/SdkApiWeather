package kameleoon.apiweather.rest.exception;

public class CityNotFoundException extends CustomException {
    public CityNotFoundException(String message) {
        super(message);
    }
}