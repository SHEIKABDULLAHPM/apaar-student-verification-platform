package com.apaar.platform.apaar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ApaarFormatValidatorTest {

    private final ApaarFormatValidator validator = new ApaarFormatValidator();

    @Test
    @DisplayName("Valid 12-digit APAAR returns valid format result")
    void validFormat() {
        ApaarFormatValidator.Result result = validator.validate("900000000001");
        assertThat(result.valid()).isTrue();
        assertThat(result.reasonCode()).isEqualTo("FORMAT_VALID");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("Blank or missing APAAR returns MISSING_APAAR")
    void missingApaar(String value) {
        ApaarFormatValidator.Result result = validator.validate(value);
        assertThat(result.valid()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("MISSING_APAAR");
    }

    @Test
    @DisplayName("Null APAAR returns MISSING_APAAR")
    void nullApaar() {
        ApaarFormatValidator.Result result = validator.validate(null);
        assertThat(result.valid()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("MISSING_APAAR");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "12345678901", "1234567890123"})
    @DisplayName("Identifiers not equal to 12 digits return INVALID_LENGTH")
    void invalidLength(String value) {
        ApaarFormatValidator.Result result = validator.validate(value);
        assertThat(result.valid()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("INVALID_LENGTH");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678901A", "123456-89012", "ABCDEF123456"})
    @DisplayName("Non-digit characters return INVALID_CHARACTERS")
    void invalidCharacters(String value) {
        ApaarFormatValidator.Result result = validator.validate(value);
        assertThat(result.valid()).isFalse();
        assertThat(result.reasonCode()).isEqualTo("INVALID_CHARACTERS");
    }
}

