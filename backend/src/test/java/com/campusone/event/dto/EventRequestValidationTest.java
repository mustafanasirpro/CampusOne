package com.campusone.event.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.event.dto.request.CreateEventRequest;
import com.campusone.event.dto.request.UpdateEventRequest;
import com.campusone.event.entity.EventVisibility;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EventRequestValidationTest {

    private static final Instant START =
            Instant.parse("2026-08-15T09:00:00Z");
    private static final Instant END =
            Instant.parse("2026-08-15T13:00:00Z");

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory()
                .getValidator();
    }

    @Test
    void createEvent_validRequest_hasNoViolations() {
        CreateEventRequest request = new CreateEventRequest(
                "Campus AI Workshop",
                "A practical workshop covering responsible AI development.",
                "COMSATS Islamabad, Seminar Hall",
                START,
                END,
                120,
                EventVisibility.PUBLIC);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void createEvent_invalidFields_hasValidationViolations() {
        CreateEventRequest request = new CreateEventRequest(
                "AI",
                "Short",
                "X",
                START,
                START,
                0,
                null);

        Set<ConstraintViolation<CreateEventRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString())
                .contains(
                        "title",
                        "description",
                        "location",
                        "capacity",
                        "visibility",
                        "timeRangeValid");
    }

    @Test
    void updateEvent_invalidSuppliedTimeRange_hasViolation() {
        UpdateEventRequest request = new UpdateEventRequest(
                null,
                null,
                null,
                END,
                START,
                null,
                null,
                null);

        assertThat(validator.validate(request))
                .extracting(violation ->
                        violation.getPropertyPath().toString())
                .contains("suppliedTimeRangeValid");
    }

    @Test
    void updateEvent_blankTextAndInvalidCapacity_hasViolations() {
        UpdateEventRequest request = new UpdateEventRequest(
                " ",
                " ",
                " ",
                null,
                null,
                0,
                null,
                null);

        assertThat(validator.validate(request))
                .extracting(violation ->
                        violation.getPropertyPath().toString())
                .contains("title", "description", "location", "capacity");
    }
}
