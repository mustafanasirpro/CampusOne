package com.campusone.security;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.campusone.config.AuthSessionProperties;
import com.campusone.config.CorsProperties;

@Configuration
@EnableConfigurationProperties({
    JwtProperties.class,
    AuthSessionProperties.class,
    CorsProperties.class
})
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RequestOriginValidationFilter requestOriginValidationFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/health")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/profiles/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/notes/my")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/notes",
                                "/api/v1/notes/*")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/discussions/questions/my")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/discussions/questions",
                                "/api/v1/discussions/questions/*",
                                "/api/v1/discussions/questions/*/answers")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/events/my",
                                "/api/v1/events/joined",
                                "/api/v1/events/*/participants/me")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/events",
                                "/api/v1/events/*")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/internships/my",
                                "/api/v1/internships/saved",
                                "/api/v1/internships/*/save/me")
                        .authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/internships",
                                "/api/v1/internships/*")
                        .permitAll()
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/api/v1/openapi/**",
                                "/api/v1/swagger-ui/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/error")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestOriginValidationFilter, JwtAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    AuthenticationManager authenticationManager(
            CampusOneUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration(
            JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    RequestOriginValidationFilter requestOriginValidationFilter(
            CorsProperties corsProperties,
            SecurityErrorResponseWriter errorResponseWriter) {
        return new RequestOriginValidationFilter(corsProperties, errorResponseWriter);
    }

    @Bean
    FilterRegistrationBean<RequestOriginValidationFilter> originFilterRegistration(
            RequestOriginValidationFilter filter) {
        FilterRegistrationBean<RequestOriginValidationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
