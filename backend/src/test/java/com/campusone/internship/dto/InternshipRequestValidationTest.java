package com.campusone.internship.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.internship.dto.request.CreateInternshipRequest;
import com.campusone.internship.dto.request.UpdateInternshipRequest;
import com.campusone.internship.entity.InternshipType;
import com.campusone.internship.entity.WorkMode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InternshipRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory()
                .getValidator();
    }

    @Test
    void createInternship_validRequest_hasNoViolations() {
        assertThat(validator.validate(validRequest())).isEmpty();
    }

    @Test
    void createInternship_invalidFields_hasViolations() {
        CreateInternshipRequest request = new CreateInternshipRequest(
                "Java",
                "X",
                "Too short",
                "X",
                null,
                null,
                null,
                new BigDecimal("-1.00"),
                "X",
                "not-a-url",
                Instant.parse("2020-01-01T00:00:00Z"));

        Set<ConstraintViolation<CreateInternshipRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString())
                .contains(
                        "title",
                        "companyName",
                        "description",
                        "location",
                        "internshipType",
                        "workMode",
                        "paid",
                        "stipendAmount",
                        "currency",
                        "applyUrl",
                        "deadline");
    }

    @Test
    void updateInternship_invalidOptionalValues_hasViolations() {
        UpdateInternshipRequest request = new UpdateInternshipRequest(
                " ",
                " ",
                " ",
                " ",
                null,
                null,
                null,
                new BigDecimal("-1.00"),
                "X",
                "javascript:alert(1)",
                Instant.parse("2020-01-01T00:00:00Z"),
                null);

        assertThat(validator.validate(request))
                .extracting(violation ->
                        violation.getPropertyPath().toString())
                .contains(
                        "title",
                        "companyName",
                        "description",
                        "location",
                        "stipendAmount",
                        "currency",
                        "applyUrl",
                        "deadline");
    }

    @Test
    void createInternship_normalizesCurrencyAndText() {
        CreateInternshipRequest request = new CreateInternshipRequest(
                "  Java Backend Intern  ",
                "  Systems Limited  ",
                "  Build and test production-quality Spring Boot services.  ",
                "  Lahore  ",
                InternshipType.SUMMER,
                WorkMode.HYBRID,
                true,
                new BigDecimal("35000.00"),
                "pkr",
                "  https://example.com/apply  ",
                Instant.parse("2099-09-30T23:59:59Z"));

        assertThat(request.title()).isEqualTo("Java Backend Intern");
        assertThat(request.currency()).isEqualTo("PKR");
        assertThat(request.applyUrl()).isEqualTo(
                "https://example.com/apply");
    }

    private CreateInternshipRequest validRequest() {
        return new CreateInternshipRequest(
                "Java Backend Intern",
                "Systems Limited",
                "Build and test production-quality Spring Boot services.",
                "Lahore",
                InternshipType.SUMMER,
                WorkMode.HYBRID,
                true,
                new BigDecimal("35000.00"),
                "PKR",
                "https://example.com/apply",
                Instant.parse("2099-09-30T23:59:59Z"));
    }
}
