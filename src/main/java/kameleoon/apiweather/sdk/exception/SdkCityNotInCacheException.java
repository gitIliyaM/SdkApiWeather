package kameleoon.apiweather.sdk.exception;

public class SdkCityNotInCacheException extends SdkCustomException {
    public SdkCityNotInCacheException(String message) {
        super(message);
    }
}