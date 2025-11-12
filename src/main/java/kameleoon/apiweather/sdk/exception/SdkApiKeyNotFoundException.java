package kameleoon.apiweather.sdk.exception;

import kameleoon.apiweather.rest.exception.CustomException;

public class SdkApiKeyNotFoundException extends CustomException {
    public SdkApiKeyNotFoundException(String message) {
        super(message);
    }
}