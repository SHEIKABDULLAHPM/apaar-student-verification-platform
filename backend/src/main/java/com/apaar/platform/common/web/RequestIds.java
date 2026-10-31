package com.apaar.platform.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

public final class RequestIds {
    private RequestIds() {
    }

    public static String current() {
        String fromMdc = MDC.get("requestId");
        if (fromMdc != null) {
            return fromMdc;
        }
        return currentRequest().map(r -> (String) r.getAttribute(RequestIdFilter.HEADER)).orElse("-");
    }

    public static Optional<HttpServletRequest> currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return Optional.of(attrs.getRequest());
        }
        return Optional.empty();
    }
}
