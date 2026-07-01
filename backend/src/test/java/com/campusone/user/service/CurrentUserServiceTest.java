package com.campusone.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.academic.entity.Department;
import com.campusone.academic.entity.University;
import com.campusone.academic.mapper.DepartmentMapper;
import com.campusone.academic.mapper.UniversityMapper;
import com.campusone.academic.repository.DepartmentRepository;
import com.campusone.academic.repository.UniversityRepository;
import com.campusone.common.exception.InvalidAcademicSelectionException;
import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.user.dto.request.PreferenceUpdateRequest;
import com.campusone.user.dto.request.UpdateProfileRequest;
import com.campusone.user.dto.request.UpdateSkillsRequest;
import com.campusone.user.dto.response.CurrentUserResponse;
import com.campusone.user.dto.response.PublicProfileResponse;
import com.campusone.user.entity.ProfileVisibility;
import com.campusone.user.entity.Skill;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.ThemePreference;
import com.campusone.user.entity.User;
import com.campusone.user.entity.UserPreference;
import com.campusone.user.mapper.CurrentUserMapper;
import com.campusone.user.mapper.PreferenceMapper;
import com.campusone.user.mapper.PublicProfileMapper;
import com.campusone.user.repository.SkillRepository;
import com.campusone.user.repository.UserPreferenceRepository;
import com.campusone.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    private static final UUID USER_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000002");
    private static final UUID UNIVERSITY_ID = UUID.fromString(
            "20000000-0000-0000-0000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString(
            "30000000-0000-0000-0000-000000000001");
    private static final UUID NEW_UNIVERSITY_ID = UUID.fromString(
            "20000000-0000-0000-0000-000000000002");
    private static final UUID NEW_DEPARTMENT_ID = UUID.fromString(
            "30000000-0000-0000-0000-000000000002");

    @Mock
    private UserRepository userRepository;

    @Mock
    private UniversityRepository universityRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SkillRepository skillRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    private CurrentUserService currentUserService;
    private User user;
    private StudentProfile profile;

    @BeforeEach
    void setUp() {
        UniversityMapper universityMapper = new UniversityMapper();
        DepartmentMapper departmentMapper = new DepartmentMapper();
        currentUserService = new CurrentUserService(
                userRepository,
                universityRepository,
                departmentRepository,
                skillRepository,
                userPreferenceRepository,
                new CurrentUserMapper(universityMapper, departmentMapper),
                new PublicProfileMapper(),
                new PreferenceMapper());

        University university = university(
                UNIVERSITY_ID,
                "COMSATS University Islamabad",
                "COMSATS");
        Department department = department(
                DEPARTMENT_ID,
                university,
                "Computer Science",
                "CS");
        user = new User("ali.khan@example.com", "$2a$12$encoded-password");
        ReflectionTestUtils.setField(user, "id", USER_ID);
        profile = new StudentProfile(user, university, department, "Ali Khan", 4);
        user.setStudentProfile(profile);
    }

    @Test
    void getCurrentUser_existingUser_returnsCompleteSafeProfile() {
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));
        when(userPreferenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        CurrentUserResponse response = currentUserService.getCurrentUser(USER_ID);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.email()).isEqualTo("ali.khan@example.com");
        assertThat(response.fullName()).isEqualTo("Ali Khan");
        assertThat(response.preferences().theme()).isEqualTo(ThemePreference.SYSTEM);
        assertThat(response.preferences().language()).isEqualTo("en");
        assertThat(response.skills()).isEmpty();
    }

    @Test
    void updateProfile_ownerRequest_updatesAcademicProfileAndPreferences() {
        University newUniversity = university(
                NEW_UNIVERSITY_ID,
                "National University of Sciences and Technology",
                "NUST");
        Department newDepartment = department(
                NEW_DEPARTMENT_ID,
                newUniversity,
                "Software Engineering",
                "SE");
        AtomicReference<UserPreference> storedPreference = new AtomicReference<>();

        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));
        when(universityRepository.findById(NEW_UNIVERSITY_ID))
                .thenReturn(Optional.of(newUniversity));
        when(departmentRepository.findByIdAndUniversityId(
                NEW_DEPARTMENT_ID,
                NEW_UNIVERSITY_ID))
                .thenReturn(Optional.of(newDepartment));
        when(userPreferenceRepository.findByUserId(USER_ID))
                .thenAnswer(ignored -> Optional.ofNullable(storedPreference.get()));
        when(userPreferenceRepository.save(any(UserPreference.class)))
                .thenAnswer(invocation -> {
                    UserPreference preference = invocation.getArgument(0);
                    storedPreference.set(preference);
                    return preference;
                });

        UpdateProfileRequest request = new UpdateProfileRequest(
                "  Ali Raza Khan  ",
                "  Building accessible student tools.  ",
                NEW_UNIVERSITY_ID,
                NEW_DEPARTMENT_ID,
                5,
                " https://cdn.example.com/avatar.png ",
                "https://cdn.example.com/cover.png",
                "  Islamabad  ",
                ProfileVisibility.PRIVATE,
                new PreferenceUpdateRequest(
                        ThemePreference.DARK,
                        "en-pk",
                        true));

        CurrentUserResponse response = currentUserService.updateProfile(USER_ID, request);

        assertThat(response.fullName()).isEqualTo("Ali Raza Khan");
        assertThat(response.bio()).isEqualTo("Building accessible student tools.");
        assertThat(response.university().id()).isEqualTo(NEW_UNIVERSITY_ID);
        assertThat(response.department().id()).isEqualTo(NEW_DEPARTMENT_ID);
        assertThat(response.semester()).isEqualTo(5);
        assertThat(response.avatarUrl()).isEqualTo("https://cdn.example.com/avatar.png");
        assertThat(response.coverImageUrl()).isEqualTo("https://cdn.example.com/cover.png");
        assertThat(response.location()).isEqualTo("Islamabad");
        assertThat(response.visibility()).isEqualTo(ProfileVisibility.PRIVATE);
        assertThat(response.preferences().theme()).isEqualTo(ThemePreference.DARK);
        assertThat(response.preferences().language()).isEqualTo("en-PK");
        assertThat(response.preferences().compactMode()).isTrue();
        verify(userPreferenceRepository).save(any(UserPreference.class));
    }

    @Test
    void updateProfile_departmentOutsideUniversity_rejectsSelection() {
        University newUniversity = university(
                NEW_UNIVERSITY_ID,
                "National University of Sciences and Technology",
                "NUST");
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));
        when(universityRepository.findById(NEW_UNIVERSITY_ID))
                .thenReturn(Optional.of(newUniversity));
        when(departmentRepository.findByIdAndUniversityId(
                DEPARTMENT_ID,
                NEW_UNIVERSITY_ID))
                .thenReturn(Optional.empty());

        UpdateProfileRequest request = new UpdateProfileRequest(
                null,
                null,
                NEW_UNIVERSITY_ID,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThatThrownBy(() -> currentUserService.updateProfile(USER_ID, request))
                .isInstanceOf(InvalidAcademicSelectionException.class)
                .hasMessageContaining("does not belong");

        assertThat(profile.getUniversity().getId()).isEqualTo(UNIVERSITY_ID);
        assertThat(profile.getDepartment().getId()).isEqualTo(DEPARTMENT_ID);
    }

    @Test
    void replaceSkills_mixedInput_reusesExistingAndCreatesMissingSkills() {
        Skill react = new Skill("React");
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));
        when(skillRepository.findAllByNormalizedNameIn(anyCollection()))
                .thenReturn(List.of(react));
        when(skillRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userPreferenceRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        CurrentUserResponse response = currentUserService.replaceSkills(
                USER_ID,
                new UpdateSkillsRequest(List.of(
                        " React ",
                        "react",
                        "",
                        "Java")));

        assertThat(response.skills()).containsExactly("Java", "React");
        assertThat(profile.getSkills())
                .extracting(Skill::getNormalizedName)
                .containsExactlyInAnyOrder("react", "java");
        verify(skillRepository).saveAll(any());
    }

    @Test
    void getPublicProfile_publicProfile_allowsAnonymousViewer() {
        profile.setBio("Computer Science student");
        profile.setVisibility(ProfileVisibility.PUBLIC);
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));

        PublicProfileResponse response =
                currentUserService.getPublicProfile(USER_ID, null);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.fullName()).isEqualTo("Ali Khan");
        assertThat(response.bio()).isEqualTo("Computer Science student");
    }

    @Test
    void getPublicProfile_privateProfile_hidesItFromOtherUsers() {
        profile.setVisibility(ProfileVisibility.PRIVATE);
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() ->
                currentUserService.getPublicProfile(USER_ID, OTHER_USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Profile was not found.");
    }

    @Test
    void getPublicProfile_privateProfile_allowsOwner() {
        profile.setVisibility(ProfileVisibility.PRIVATE);
        when(userRepository.findProfileById(USER_ID)).thenReturn(Optional.of(user));

        PublicProfileResponse response =
                currentUserService.getPublicProfile(USER_ID, USER_ID);

        assertThat(response.userId()).isEqualTo(USER_ID);
    }

    private University university(
            UUID id,
            String name,
            String shortName) {
        University university = new University(name, shortName, "Islamabad");
        ReflectionTestUtils.setField(university, "id", id);
        return university;
    }

    private Department department(
            UUID id,
            University university,
            String name,
            String code) {
        Department department = new Department(university, name, code);
        ReflectionTestUtils.setField(department, "id", id);
        return department;
    }
}
