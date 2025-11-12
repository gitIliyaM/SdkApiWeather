package kameleoon.apiweather.sdk.exception;

public class SdkCustomException extends RuntimeException {
    public SdkCustomException(String message) {
        super(message);
    }
}