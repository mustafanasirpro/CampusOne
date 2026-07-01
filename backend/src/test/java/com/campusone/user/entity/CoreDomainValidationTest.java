package com.campusone.user.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.academic.entity.Course;
import com.campusone.academic.entity.Department;
import com.campusone.academic.entity.University;
import com.campusone.user.dto.request.CreateUserRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CoreDomainValidationTest {

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
    void coreDomain_whenValuesAreValid_hasNoValidationViolations() {
        University university = new University(
                "COMSATS University Islamabad",
                "COMSATS",
                "Islamabad");
        Department department = new Department(
                university,
                "Computer Science",
                "CS");
        Course course = new Course(
                department,
                "CSC-241",
                "Object Oriented Programming");
        course.setRecommendedSemester(3);

        User user = new User("student@example.edu.pk", "a".repeat(60));
        StudentProfile profile = new StudentProfile(
                user,
                university,
                department,
                "Ali Khan",
                3);

        assertThat(validator.validate(university)).isEmpty();
        assertThat(validator.validate(department)).isEmpty();
        assertThat(validator.validate(course)).isEmpty();
        assertThat(validator.validate(user)).isEmpty();
        assertThat(validator.validate(profile)).isEmpty();
    }

    @Test
    void createUserRequest_whenCredentialsAreInvalid_reportsBothFields() {
        CreateUserRequest request = new CreateUserRequest("not-an-email", "short");

        Set<ConstraintViolation<CreateUserRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("email", "password");
    }

    @Test
    void studentProfile_whenSemesterAndNameAreInvalid_reportsBothFields() {
        University university = new University(
                "COMSATS University Islamabad",
                "COMSATS",
                "Islamabad");
        Department department = new Department(
                university,
                "Computer Science",
                "CS");
        User user = new User("student@example.edu.pk", "a".repeat(60));
        StudentProfile profile = new StudentProfile(
                user,
                university,
                department,
                "A",
                9);

        Set<ConstraintViolation<StudentProfile>> violations =
                validator.validate(profile);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("fullName", "semester");
    }

    @Test
    void relationshipHelpers_whenUsed_keepBothSidesConsistent() {
        University university = new University(
                "COMSATS University Islamabad",
                "COMSATS",
                "Islamabad");
        Department department = new Department(
                university,
                "Computer Science",
                "CS");
        Course course = new Course(
                department,
                "CSC-241",
                "Object Oriented Programming");
        User user = new User("student@example.edu.pk", "a".repeat(60));
        StudentProfile profile = new StudentProfile(
                user,
                university,
                department,
                "Ali Khan",
                3);

        university.addDepartment(department);
        department.addCourse(course);
        user.setStudentProfile(profile);

        assertThat(university.getDepartments()).containsExactly(department);
        assertThat(department.getUniversity()).isSameAs(university);
        assertThat(department.getCourses()).containsExactly(course);
        assertThat(course.getDepartment()).isSameAs(department);
        assertThat(user.getStudentProfile()).isSameAs(profile);
        assertThat(profile.getUser()).isSameAs(user);
    }
}
