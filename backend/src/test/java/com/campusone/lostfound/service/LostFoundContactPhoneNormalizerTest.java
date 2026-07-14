package com.campusone.lostfound.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campusone.lostfound.exception.LostFoundConflictException;
import org.junit.jupiter.api.Test;

class LostFoundContactPhoneNormalizerTest {

    private final LostFoundContactPhoneNormalizer normalizer =
            new LostFoundContactPhoneNormalizer();

    @Test
    void normalizesValidInternationalNumber() {
        assertThat(normalizer.normalize("+441234567890"))
                .isEqualTo("+441234567890");
    }

    @Test
    void normalizesPakistaniLocalMobileNumber() {
        assertThat(normalizer.normalize("03001234567"))
                .isEqualTo("+923001234567");
    }

    @Test
    void removesAllowedSeparatorsBeforeValidation() {
        assertThat(normalizer.normalize("+92 (300) 123-4567"))
                .isEqualTo("+923001234567");
    }

    @Test
    void rejectsEmptyPhoneNumber() {
        assertInvalid("");
    }

    @Test
    void rejectsLettersAndExtensions() {
        assertInvalid("+923001234567 ext 2");
    }

    @Test
    void rejectsTooShortPhoneNumber() {
        assertInvalid("+1234567");
    }

    @Test
    void rejectsTooLongPhoneNumber() {
        assertInvalid("+1234567890123456");
    }

    @Test
    void rejectsMalformedPakistaniLocalNumber() {
        assertInvalid("031234567");
    }

    @Test
    void rejectsMultiplePlusSigns() {
        assertInvalid("++923001234567");
    }

    @Test
    void rejectsControlCharacters() {
        assertInvalid("+923001234567\n");
    }

    private void assertInvalid(String phone) {
        assertThatThrownBy(() -> normalizer.normalize(phone))
                .isInstanceOf(LostFoundConflictException.class)
                .hasMessage("Enter a valid handover contact number.");
    }
}
