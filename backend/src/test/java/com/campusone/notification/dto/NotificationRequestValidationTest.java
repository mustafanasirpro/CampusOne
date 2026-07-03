package com.campusone.notification.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.notification.dto.request.CreateNotificationRequest;
import com.campusone.notification.entity.NotificationTargetType;
import com.campusone.notification.entity.NotificationType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class NotificationRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory()
                .getValidator();
    }

    @Test
    void createNotification_validRequest_hasNoViolations() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                NotificationType.USER_REMINDER,
                "Revise database normalization",
                "Review the third normal form examples before class.",
                NotificationTargetType.USER,
                UUID.randomUUID(),
                "/dashboard/notifications");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void createNotification_invalidContent_hasViolations() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                null,
                "No",
                " ",
                null,
                null,
                "x".repeat(1001));

        Set<ConstraintViolation<CreateNotificationRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(violation ->
                        violation.getPropertyPath().toString())
                .contains("type", "title", "message", "actionUrl");
    }

    @Test
    void createNotification_targetIdWithoutType_hasViolation() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                NotificationType.USER_REMINDER,
                "Review lecture notes",
                "Review the lecture notes before tomorrow's class.",
                null,
                UUID.randomUUID(),
                null);

        assertThat(validator.validate(request))
                .extracting(violation ->
                        violation.getPropertyPath().toString())
                .contains("targetValid");
    }

    @Test
    void createNotification_normalizesTextAndAllowsRelativeActionUrl() {
        CreateNotificationRequest request = new CreateNotificationRequest(
                NotificationType.USER_REMINDER,
                "  Review lecture notes  ",
                "  Review the lecture notes before tomorrow's class.  ",
                null,
                null,
                "  /notes/my  ");

        assertThat(request.title()).isEqualTo("Review lecture notes");
        assertThat(request.message()).isEqualTo(
                "Review the lecture notes before tomorrow's class.");
        assertThat(request.actionUrl()).isEqualTo("/notes/my");
    }
}
