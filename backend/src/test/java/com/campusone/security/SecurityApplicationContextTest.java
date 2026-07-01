package com.campusone.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.campusone.academic.repository.DepartmentRepository;
import com.campusone.academic.repository.UniversityRepository;
import com.campusone.auth.repository.RefreshTokenRepository;
import com.campusone.user.repository.RoleRepository;
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
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private SecurityConfig securityConfig;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private Clock clock;

    @Test
    void applicationContext_securityBeansLoadWithoutCircularDependency() {
        assertThat(securityConfig).isNotNull();
        assertThat(jwtAuthenticationFilter).isNotNull();
        assertThat(jwtService).isNotNull();
        assertThat(clock).isNotNull();
    }
}
