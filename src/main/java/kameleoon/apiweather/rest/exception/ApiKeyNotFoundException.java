package kameleoon.apiweather.rest.exception;

public class ApiKeyNotFoundException extends CustomException {
    public ApiKeyNotFoundException(String message) {
        super(message);
    }
}