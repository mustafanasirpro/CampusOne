package com.campusone.user.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.user.dto.request.PreferenceUpdateRequest;
import com.campusone.user.dto.request.UpdateProfileRequest;
import com.campusone.user.dto.request.UpdateSkillsRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProfileRequestValidationTest {

    private static jakarta.validation.ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void updateProfile_outOfRangeAndOversizedFields_reportsValidationErrors() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "A",
                "b".repeat(501),
                null,
                null,
                9,
                "ftp://files.example.com/avatar.png",
                "not-a-url",
                "L".repeat(101),
                null,
                new PreferenceUpdateRequest(null, "english", null));

        Set<ConstraintViolation<UpdateProfileRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains(
                        "fullName",
                        "bio",
                        "semester",
                        "avatarUrl",
                        "coverImageUrl",
                        "location",
                        "preferences.language");
    }

    @Test
    void updateProfile_emptyOptionalStrings_areAcceptedForClearing() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                null,
                "",
                null,
                null,
                null,
                "",
                "",
                "",
                null,
                null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void updateSkills_duplicatesAndBlankValues_areNormalized() {
        UpdateSkillsRequest request =
                new UpdateSkillsRequest(List.of(" React ", "react", "", " Java "));

        assertThat(request.skills()).containsExactly("React", "Java");
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void updateSkills_moreThanTwentyUniqueSkills_isRejected() {
        List<String> skills = IntStream.rangeClosed(1, 21)
                .mapToObj(number -> "Skill " + number)
                .toList();
        UpdateSkillsRequest request = new UpdateSkillsRequest(skills);

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("skills");
    }

    @Test
    void updateSkills_tooShortSkill_isRejected() {
        UpdateSkillsRequest request = new UpdateSkillsRequest(List.of("A"));

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("skills[0].<list element>");
    }
}
