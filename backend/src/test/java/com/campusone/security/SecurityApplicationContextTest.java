package com.campusone.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.academic.repository.DepartmentRepository;
import com.campusone.academic.repository.CourseRepository;
import com.campusone.academic.repository.UniversityRepository;
import com.campusone.auth.repository.RefreshTokenRepository;
import com.campusone.note.repository.FileAssetRepository;
import com.campusone.note.repository.NoteBookmarkRepository;
import com.campusone.note.repository.NoteDownloadEventRepository;
import com.campusone.note.repository.NoteModerationActionRepository;
import com.campusone.note.repository.NoteRatingRepository;
import com.campusone.note.repository.NoteRepository;
import com.campusone.note.repository.NoteVersionRepository;
import com.campusone.note.repository.TagRepository;
import com.campusone.note.repository.UploadQuotaRepository;
import com.campusone.user.repository.RoleRepository;
import com.campusone.user.repository.SkillRepository;
import com.campusone.user.repository.UserPreferenceRepository;
import com.campusone.user.repository.UserRepository;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class SecurityApplicationContextTest {

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private UniversityRepository universityRepository;

    @MockitoBean
    private DepartmentRepository departmentRepository;

    @MockitoBean
    private CourseRepository courseRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private SkillRepository skillRepository;

    @MockitoBean
    private UserPreferenceRepository userPreferenceRepository;

    @MockitoBean
    private NoteRepository noteRepository;

    @MockitoBean
    private FileAssetRepository fileAssetRepository;

    @MockitoBean
    private TagRepository tagRepository;

    @MockitoBean
    private NoteVersionRepository noteVersionRepository;

    @MockitoBean
    private NoteRatingRepository noteRatingRepository;

    @MockitoBean
    private NoteBookmarkRepository noteBookmarkRepository;

    @MockitoBean
    private NoteDownloadEventRepository noteDownloadEventRepository;

    @MockitoBean
    private NoteModerationActionRepository noteModerationActionRepository;

    @MockitoBean
    private UploadQuotaRepository uploadQuotaRepository;

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private NoteManagementAuthorizationFilter
            noteManagementAuthorizationFilter;

    @Autowired
    private Clock clock;

    @Test
    void applicationContext_securityBeansLoadWithoutCircularDependency() {
        assertThat(securityConfig).isNotNull();
        assertThat(jwtAuthenticationFilter).isNotNull();
        assertThat(jwtService).isNotNull();
        assertThat(noteManagementAuthorizationFilter).isNotNull();
        assertThat(clock).isNotNull();
    }
}
