package com.campusone.auth.controller;

import com.campusone.auth.dto.request.LoginRequest;
import com.campusone.auth.dto.request.RegisterRequest;
import com.campusone.auth.dto.response.AuthResponse;
import com.campusone.auth.dto.response.UserSummaryResponse;
import com.campusone.auth.service.AuthenticationResult;
import com.campusone.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    public AuthController(
            AuthService authService,
            RefreshTokenCookieFactory refreshTokenCookieFactory) {
        this.authService = authService;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
    }

    @PostMapping("/register")
    @Operation(summary = "Create a student account")
    public ResponseEntity<UserSummaryResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in and issue a short-lived access token")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return authenticatedResponse(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate the refresh token and issue a new access token")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
        return authenticatedResponse(
                authService.refresh(refreshTokenCookieFactory.readToken(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke the current refresh-token session")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        authService.logout(refreshTokenCookieFactory.readToken(request));
        return ResponseEntity.noContent()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieFactory.clear().toString())
                .build();
    }

    private ResponseEntity<AuthResponse> authenticatedResponse(AuthenticationResult result) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.SET_COOKIE,
                        refreshTokenCookieFactory.create(
                                result.refreshToken(),
                                result.refreshTokenExpiresAt())
                                .toString())
                .body(result.response());
    }
}
