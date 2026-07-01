package com.campusone.user.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.academic.entity.Course;
import com.campusone.academic.entity.Department;
import com.campusone.academic.entity.University;
import com.campusone.academic.mapper.CourseMapper;
import com.campusone.academic.mapper.DepartmentMapper;
import com.campusone.academic.mapper.UniversityMapper;
import com.campusone.user.dto.response.StudentProfileResponse;
import com.campusone.user.dto.response.UserResponse;
import com.campusone.user.entity.Role;
import com.campusone.user.entity.RoleName;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import org.junit.jupiter.api.Test;

class CoreMapperTest {

    private final UniversityMapper universityMapper = new UniversityMapper();
    private final DepartmentMapper departmentMapper = new DepartmentMapper();
    private final CourseMapper courseMapper = new CourseMapper();
    private final UserMapper userMapper = new UserMapper();
    private final StudentProfileMapper profileMapper =
            new StudentProfileMapper(universityMapper, departmentMapper);

    @Test
    void mappers_whenGivenCoreDomain_returnSafeResponseDtos() {
        University university = new University(
                "COMSATS University Islamabad",
                "COMSATS",
                "Islamabad");
        university.setWebsite("https://www.comsats.edu.pk");
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
        user.addRole(new Role(RoleName.STUDENT));
        StudentProfile profile = new StudentProfile(
                user,
                university,
                department,
                "Ali Khan",
                3);
        profile.setBio("Computer Science student");
        profile.setTotalXp(250);
        user.setStudentProfile(profile);

        UserResponse userResponse = userMapper.toResponse(user);
        StudentProfileResponse profileResponse = profileMapper.toResponse(profile);

        assertThat(userResponse.email()).isEqualTo("student@example.edu.pk");
        assertThat(userResponse.roles()).containsExactly(RoleName.STUDENT);
        assertThat(profileResponse.fullName()).isEqualTo("Ali Khan");
        assertThat(profileResponse.university().shortName()).isEqualTo("COMSATS");
        assertThat(profileResponse.department().code()).isEqualTo("CS");
        assertThat(profileResponse.totalXp()).isEqualTo(250);
        assertThat(courseMapper.toResponse(course).courseCode()).isEqualTo("CSC-241");
    }
}
