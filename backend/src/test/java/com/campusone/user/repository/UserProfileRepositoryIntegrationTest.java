package com.campusone.user.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.academic.entity.Department;
import com.campusone.academic.entity.University;
import com.campusone.academic.repository.DepartmentRepository;
import com.campusone.academic.repository.UniversityRepository;
import com.campusone.user.entity.AccountStatus;
import com.campusone.user.entity.ProfileVisibility;
import com.campusone.user.entity.Skill;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.ThemePreference;
import com.campusone.user.entity.User;
import com.campusone.user.entity.UserPreference;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude="
            + "org.springframework.boot.autoconfigure.security.servlet."
            + "UserDetailsServiceAutoConfiguration",
    "spring.flyway.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
@Transactional
class UserProfileRepositoryIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private UserPreferenceRepository userPreferenceRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void profileEntities_flywaySchema_persistAndLoadCompleteProfile() {
        University university = universityRepository.save(
                new University(
                        "Profile Test University",
                        "PTU",
                        "Islamabad"));
        Department department = departmentRepository.save(
                new Department(
                        university,
                        "Computer Science",
                        "CS"));
        Skill skill = skillRepository.save(new Skill("Spring Boot"));

        User user = new User(
                "profile.integration@example.com",
                "$2a$12$aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        user.setAccountStatus(AccountStatus.ACTIVE);
        StudentProfile profile = new StudentProfile(
                user,
                university,
                department,
                "Ayesha Malik",
                5);
        profile.setBio("Backend engineering student");
        profile.setCoverImageUrl("https://cdn.example.com/cover.png");
        profile.setLocation("Islamabad");
        profile.setVisibility(ProfileVisibility.PRIVATE);
        profile.replaceSkills(java.util.List.of(skill));
        user.setStudentProfile(profile);
        User savedUser = userRepository.saveAndFlush(user);

        UserPreference preference = new UserPreference(savedUser);
        preference.update(ThemePreference.DARK, "en-PK", true);
        userPreferenceRepository.saveAndFlush(preference);
        entityManager.clear();

        User loadedUser = userRepository.findProfileById(savedUser.getId()).orElseThrow();
        UserPreference loadedPreference = userPreferenceRepository
                .findByUserId(savedUser.getId())
                .orElseThrow();

        assertThat(loadedUser.getStudentProfile().getCoverImageUrl())
                .isEqualTo("https://cdn.example.com/cover.png");
        assertThat(loadedUser.getStudentProfile().getLocation()).isEqualTo("Islamabad");
        assertThat(loadedUser.getStudentProfile().getVisibility())
                .isEqualTo(ProfileVisibility.PRIVATE);
        assertThat(loadedUser.getStudentProfile().getSkills())
                .extracting(Skill::getName)
                .containsExactly("Spring Boot");
        assertThat(loadedPreference.getTheme()).isEqualTo(ThemePreference.DARK);
        assertThat(loadedPreference.getLanguage()).isEqualTo("en-PK");
        assertThat(loadedPreference.isCompactMode()).isTrue();
    }
}
