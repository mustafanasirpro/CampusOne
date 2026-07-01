package com.campusone.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campusone.academic.entity.Department;
import com.campusone.academic.entity.University;
import com.campusone.academic.repository.DepartmentRepository;
import com.campusone.academic.repository.UniversityRepository;
import com.campusone.auth.dto.request.LoginRequest;
import com.campusone.auth.dto.request.RegisterRequest;
import com.campusone.auth.dto.response.AuthResponse;
import com.campusone.auth.dto.response.UserSummaryResponse;
import com.campusone.auth.mapper.AuthMapper;
import com.campusone.common.exception.DuplicateEmailException;
import com.campusone.common.exception.InvalidAcademicSelectionException;
import com.campusone.config.AuthSessionProperties;
import com.campusone.security.CampusOneUserPrincipal;
import com.campusone.security.JwtService;
import com.campusone.user.entity.AccountStatus;
import com.campusone.user.entity.Role;
import com.campusone.user.entity.RoleName;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import com.campusone.user.mapper.RoleMapper;
import com.campusone.user.repository.RoleRepository;
import com.campusone.user.repository.UserRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final UUID UNIVERSITY_ID = UUID.fromString(
            "10000000-0000-0000-0000-000000000001");
    private static final UUID DEPARTMENT_ID = UUID.fromString(
            "20000000-0000-0000-0000-000000000001");
    private static final UUID USER_ID = UUID.fromString(
            "30000000-0000-0000-0000-000000000001");

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UniversityRepository universityRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthService authService;
    private Clock clock;
    private AuthSessionProperties authSessionProperties;
    private University university;
    private Department department;
    private Role studentRole;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                Instant.parse("2026-07-01T12:00:00Z"),
                ZoneOffset.UTC);
        authSessionProperties = new AuthSessionProperties();
        authSessionProperties.setMaxLoginAttempts(5);
        authSessionProperties.setAccountLockDuration(Duration.ofMinutes(15));
        authService = new AuthService(
                userRepository,
                roleRepository,
                universityRepository,
                departmentRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                refreshTokenService,
                new AuthMapper(new RoleMapper()),
                authSessionProperties,
                clock);
        university = new University("COMSATS University Islamabad", "COMSATS", "Islamabad");
        department = new Department(university, "Computer Science", "CS");
        studentRole = new Role(RoleName.STUDENT);
    }

    @Test
    void register_validRequest_createsActiveStudentAccount() {
        RegisterRequest request = registrationRequest();
        when(userRepository.existsByEmailIgnoreCase("ali.khan@example.com")).thenReturn(false);
        when(universityRepository.findById(UNIVERSITY_ID)).thenReturn(Optional.of(university));
        when(departmentRepository.findByIdAndUniversityId(DEPARTMENT_ID, UNIVERSITY_ID))
                .thenReturn(Optional.of(department));
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
        when(passwordEncoder.encode("SecurePass1")).thenReturn("$2a$12$encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserSummaryResponse response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmail()).isEqualTo("ali.khan@example.com");
        assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$12$encoded-password");
        assertThat(savedUser.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(savedUser.isEmailVerified()).isFalse();
        assertThat(savedUser.getRoles()).containsExactly(studentRole);
        assertThat(savedUser.getStudentProfile().getFullName()).isEqualTo("Ali Khan");
        assertThat(savedUser.getStudentProfile().getUniversity()).isSameAs(university);
        assertThat(savedUser.getStudentProfile().getDepartment()).isSameAs(department);
        assertThat(savedUser.getStudentProfile().getSemester()).isEqualTo(4);
        assertThat(response.email()).isEqualTo("ali.khan@example.com");
        assertThat(response.roles()).containsExactly("STUDENT");
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void register_existingEmail_throwsDuplicateEmail() {
        when(userRepository.existsByEmailIgnoreCase("ali.khan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registrationRequest()))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_departmentOutsideUniversity_rejectsAcademicSelection() {
        when(userRepository.existsByEmailIgnoreCase("ali.khan@example.com")).thenReturn(false);
        when(universityRepository.findById(UNIVERSITY_ID)).thenReturn(Optional.of(university));
        when(departmentRepository.findByIdAndUniversityId(DEPARTMENT_ID, UNIVERSITY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.register(registrationRequest()))
                .isInstanceOf(InvalidAcademicSelectionException.class)
                .hasMessageContaining("does not belong");

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_validCredentials_returnsAccessTokenAndSafeUserSummary() {
        CampusOneUserPrincipal principal = new CampusOneUserPrincipal(
                USER_ID,
                "ali.khan@example.com",
                "$2a$12$encoded-password",
                AccountStatus.ACTIVE,
                Set.of("STUDENT"));
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        principal.getAuthorities());
        User user = userWithProfile();
        user.recordFailedLogin(
                clock.instant(),
                5,
                Duration.ofMinutes(15));

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(userRepository.findDetailedById(USER_ID)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(principal)).thenReturn("signed.jwt.token");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(refreshTokenService.issue(user)).thenReturn(new IssuedRefreshToken(
                "A".repeat(43),
                java.time.Instant.parse("2026-07-08T12:00:00Z"),
                user));

        AuthenticationResult result = authService.login(
                new LoginRequest("  ALI.KHAN@EXAMPLE.COM ", "SecurePass1"));
        AuthResponse response = result.response();

        assertThat(response.accessToken()).isEqualTo("signed.jwt.token");
        assertThat(response.expiresIn()).isEqualTo(900);
        assertThat(response.user().fullName()).isEqualTo("Ali Khan");
        assertThat(response.user().email()).isEqualTo("ali.khan@example.com");
        assertThat(response.user().roles()).containsExactly("STUDENT");
        assertThat(result.refreshToken()).isEqualTo("A".repeat(43));
        assertThat(result.refreshTokenExpiresAt())
                .isEqualTo(java.time.Instant.parse("2026-07-08T12:00:00Z"));
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authenticationCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(authenticationCaptor.capture());
        assertThat(authenticationCaptor.getValue().getPrincipal())
                .isEqualTo("ali.khan@example.com");
    }

    @Test
    void login_wrongPassword_propagatesInvalidCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("ali.khan@example.com", "WrongPass1")))
                .isInstanceOf(BadCredentialsException.class);

        verify(jwtService, never()).generateAccessToken(any());
        verify(refreshTokenService, never()).issue(any());
    }

    @Test
    void login_repeatedWrongPassword_locksKnownAccountTemporarily() {
        User user = userWithProfile();
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));
        when(userRepository.findByEmailIgnoreCase("ali.khan@example.com"))
                .thenReturn(Optional.of(user));

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThatThrownBy(() -> authService.login(
                    new LoginRequest("ali.khan@example.com", "WrongPass1")))
                    .isInstanceOf(BadCredentialsException.class);
        }

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLockedUntil())
                .isEqualTo(clock.instant().plus(Duration.ofMinutes(15)));
        assertThat(user.isLoginLockedAt(clock.instant())).isTrue();
    }

    @Test
    void refresh_validSession_rotatesRefreshTokenAndIssuesAccessToken() {
        User user = userWithProfile();
        IssuedRefreshToken rotatedToken = new IssuedRefreshToken(
                "B".repeat(43),
                java.time.Instant.parse("2026-07-08T12:00:00Z"),
                user);
        when(refreshTokenService.rotate("A".repeat(43))).thenReturn(rotatedToken);
        when(jwtService.generateAccessToken(any())).thenReturn("new.access.token");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);

        AuthenticationResult result = authService.refresh("A".repeat(43));

        assertThat(result.response().accessToken()).isEqualTo("new.access.token");
        assertThat(result.response().user().email()).isEqualTo("ali.khan@example.com");
        assertThat(result.refreshToken()).isEqualTo("B".repeat(43));
        verify(refreshTokenService).rotate("A".repeat(43));
    }

    @Test
    void logout_currentSession_revokesOnlyPresentedRefreshToken() {
        authService.logout("A".repeat(43));

        verify(refreshTokenService).revoke("A".repeat(43));
    }

    private RegisterRequest registrationRequest() {
        return new RegisterRequest(
                " Ali Khan ",
                "  ALI.KHAN@EXAMPLE.COM ",
                "SecurePass1",
                UNIVERSITY_ID,
                DEPARTMENT_ID,
                4);
    }

    private User userWithProfile() {
        User user = new User("ali.khan@example.com", "$2a$12$encoded-password");
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.addRole(studentRole);
        user.setStudentProfile(new StudentProfile(
                user,
                university,
                department,
                "Ali Khan",
                4));
        return user;
    }
}
