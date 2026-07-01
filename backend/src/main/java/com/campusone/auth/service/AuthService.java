package com.campusone.auth.service;

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
import com.campusone.common.exception.InvalidRefreshTokenException;
import com.campusone.common.exception.InvalidAcademicSelectionException;
import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.common.util.EmailNormalizer;
import com.campusone.config.AuthSessionProperties;
import com.campusone.security.CampusOneUserPrincipal;
import com.campusone.security.JwtService;
import com.campusone.user.entity.AccountStatus;
import com.campusone.user.entity.Role;
import com.campusone.user.entity.RoleName;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import com.campusone.user.repository.RoleRepository;
import com.campusone.user.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UniversityRepository universityRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthMapper authMapper;
    private final AuthSessionProperties authSessionProperties;
    private final Clock clock;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UniversityRepository universityRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            AuthMapper authMapper,
            AuthSessionProperties authSessionProperties,
            Clock clock) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.universityRepository = universityRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.authMapper = authMapper;
        this.authSessionProperties = authSessionProperties;
        this.clock = clock;
    }

    @Transactional
    public UserSummaryResponse register(RegisterRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateEmailException();
        }

        University university = universityRepository.findById(request.universityId())
                .orElseThrow(() -> new ResourceNotFoundException("University"));
        if (!university.isActive()) {
            throw new InvalidAcademicSelectionException(
                    "The selected university is not accepting registrations.");
        }

        Department department = departmentRepository
                .findByIdAndUniversityId(request.departmentId(), request.universityId())
                .orElseThrow(() -> new InvalidAcademicSelectionException(
                        "The selected department does not belong to the selected university."));
        if (!department.isActive()) {
            throw new InvalidAcademicSelectionException(
                    "The selected department is not accepting registrations.");
        }

        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
                .orElseThrow(() -> new IllegalStateException(
                        "The required STUDENT role is not configured."));

        User user = new User(normalizedEmail, passwordEncoder.encode(request.password()));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setEmailVerified(false);
        user.addRole(studentRole);

        StudentProfile profile = new StudentProfile(
                user,
                university,
                department,
                request.fullName(),
                request.semester());
        user.setStudentProfile(profile);

        User savedUser = userRepository.save(user);
        return authMapper.toUserSummary(savedUser);
    }

    @Transactional(noRollbackFor = AuthenticationException.class)
    public AuthenticationResult login(LoginRequest request) {
        String normalizedEmail = EmailNormalizer.normalize(request.email());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            normalizedEmail,
                            request.password()));
        } catch (AuthenticationException exception) {
            recordFailedLogin(normalizedEmail);
            throw exception;
        }
        CampusOneUserPrincipal principal =
                (CampusOneUserPrincipal) authentication.getPrincipal();

        User user = userRepository.findDetailedById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        user.resetFailedLoginAttempts();
        LOGGER.info("Authentication succeeded for user {}", user.getId());
        IssuedRefreshToken refreshToken = refreshTokenService.issue(user);
        return authenticationResult(principal, user, refreshToken);
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthenticationResult refresh(String rawRefreshToken) {
        IssuedRefreshToken refreshToken = refreshTokenService.rotate(rawRefreshToken);
        User user = refreshToken.user();
        CampusOneUserPrincipal principal = CampusOneUserPrincipal.from(user);
        return authenticationResult(principal, user, refreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private AuthenticationResult authenticationResult(
            CampusOneUserPrincipal principal,
            User user,
            IssuedRefreshToken refreshToken) {
        AuthResponse response = new AuthResponse(
                jwtService.generateAccessToken(principal),
                jwtService.getAccessTokenTtlSeconds(),
                authMapper.toUserSummary(user));
        return new AuthenticationResult(
                response,
                refreshToken.token(),
                refreshToken.expiresAt());
    }

    private void recordFailedLogin(String normalizedEmail) {
        Instant now = clock.instant();
        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .ifPresentOrElse(user -> {
                    boolean alreadyLocked = user.isLoginLockedAt(now);
                    user.recordFailedLogin(
                            now,
                            authSessionProperties.getMaxLoginAttempts(),
                            authSessionProperties.getAccountLockDuration());
                    if (!alreadyLocked && user.isLoginLockedAt(now)) {
                        LOGGER.warn(
                                "Account locked after repeated authentication failures for user {}",
                                user.getId());
                    } else {
                        LOGGER.info("Authentication failed for user {}", user.getId());
                    }
                }, () -> LOGGER.info("Authentication failed for an unknown account"));
    }
}
