package com.apaar.platform.apaar;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Structural format validation only. This is NOT authoritative APAAR verification.
 * The 12-digit rule is a documented prototype assumption and is configurable in
 * docs/INTEGRATIONS.md; the authorized APAAR specification may differ.
 */
@Component
public class ApaarFormatValidator {
    private static final Pattern FORMAT = Pattern.compile("^[0-9]{12}$");

    public record Result(boolean valid, String reasonCode, String message) {
    }

    public Result validate(String value) {
        if (value == null || value.isBlank()) {
            return new Result(false, "MISSING_APAAR", "APAAR identifier is missing");
        }
        String trimmed = value.trim();
        if (trimmed.length() != 12) {
            return new Result(false, "INVALID_LENGTH", "APAAR identifier must contain exactly 12 digits");
        }
        if (!FORMAT.matcher(trimmed).matches()) {
            return new Result(false, "INVALID_CHARACTERS", "APAAR identifier must contain digits only");
        }
        return new Result(true, "FORMAT_VALID", "Format check passed; this is not authoritative verification");
    }
}
