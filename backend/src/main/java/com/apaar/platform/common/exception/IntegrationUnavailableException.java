package com.apaar.platform.common.exception;

import org.springframework.http.HttpStatus;

public class IntegrationUnavailableException extends ApiException {
    public IntegrationUnavailableException(String service) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE",
                service + " is currently unavailable; no result was recorded");
    }
}
