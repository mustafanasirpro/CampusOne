package com.campusone.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.auth.dto.request.LoginRequest;
import com.campusone.auth.dto.request.RegisterRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AuthRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void registerRequest_weakPassword_failsPasswordPolicy() {
        RegisterRequest request = new RegisterRequest(
                "Ali Khan",
                "ali.khan@example.com",
                "lowercase1",
                UUID.randomUUID(),
                UUID.randomUUID(),
                4);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password")
                        && violation.getMessage().contains("uppercase"));
    }

    @Test
    void registerRequest_validInput_passesValidationAndNormalizesIdentityFields() {
        RegisterRequest request = new RegisterRequest(
                "  Ali Khan  ",
                "  ALI.KHAN@EXAMPLE.COM  ",
                "SecurePass1",
                UUID.randomUUID(),
                UUID.randomUUID(),
                4);

        assertThat(validator.validate(request)).isEmpty();
        assertThat(request.fullName()).isEqualTo("Ali Khan");
        assertThat(request.email()).isEqualTo("ali.khan@example.com");
    }

    @Test
    void loginRequest_email_isNormalizedWithoutChangingPassword() {
        LoginRequest request = new LoginRequest(
                "  ALI.KHAN@EXAMPLE.COM ",
                " PasswordWithSpace1 ");

        assertThat(request.email()).isEqualTo("ali.khan@example.com");
        assertThat(request.password()).isEqualTo(" PasswordWithSpace1 ");
    }

    @Test
    void registerRequest_passwordOverBcryptUtf8Limit_failsValidation() {
        RegisterRequest request = new RegisterRequest(
                "Ali Khan",
                "ali.khan@example.com",
                "Secure1" + "é".repeat(33),
                UUID.randomUUID(),
                UUID.randomUUID(),
                4);

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password")
                        && violation.getMessage().contains("72 bytes"));
    }
}
