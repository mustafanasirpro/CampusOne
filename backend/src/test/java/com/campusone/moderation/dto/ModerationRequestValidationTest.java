package com.campusone.moderation.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.moderation.dto.request.CreateReportRequest;
import com.campusone.moderation.dto.request.DismissReportRequest;
import com.campusone.moderation.dto.request.ResolveReportRequest;
import com.campusone.moderation.entity.ModerationTargetType;
import com.campusone.moderation.entity.ReportReason;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ModerationRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory()
                .getValidator();
    }

    @Test
    void validRequests_haveNoViolations() {
        var report = new CreateReportRequest(
                ModerationTargetType.NOTE,
                UUID.randomUUID(),
                ReportReason.SPAM,
                "Repeated promotional content.");

        assertThat(validator.validate(report)).isEmpty();
        assertThat(validator.validate(
                new ResolveReportRequest(
                        "Reviewed and resolved."))).isEmpty();
        assertThat(validator.validate(
                new DismissReportRequest(
                        "No policy violation found."))).isEmpty();
    }

    @Test
    void invalidReportRequest_isRejected() {
        var request = new CreateReportRequest(
                null,
                null,
                null,
                "x".repeat(1001));

        assertThat(validator.validate(request)).hasSize(4);
    }

    @Test
    void invalidResolutionNotes_areRejected() {
        assertThat(validator.validate(
                new ResolveReportRequest("x"))).isNotEmpty();
        assertThat(validator.validate(
                new DismissReportRequest(
                        "x".repeat(1001)))).isNotEmpty();
    }
}
