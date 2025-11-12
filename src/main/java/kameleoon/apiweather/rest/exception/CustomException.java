package kameleoon.apiweather.rest.exception;

public class CustomException extends RuntimeException {
    public CustomException(String message) {
        super(message);
    }
}