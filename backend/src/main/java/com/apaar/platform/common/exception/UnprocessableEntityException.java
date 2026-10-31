package com.apaar.platform.common.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class UnprocessableEntityException extends ApiException {
    public UnprocessableEntityException(String code, String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message, Map.of());
    }

    public UnprocessableEntityException(String code, String message, Map<String, Object> details) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code, message, details);
    }
}
