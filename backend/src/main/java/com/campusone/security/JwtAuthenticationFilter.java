package com.campusone.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String AUTH_ERROR_CODE =
            JwtAuthenticationFilter.class.getName() + ".errorCode";
    static final String AUTH_ERROR_MESSAGE =
            JwtAuthenticationFilter.class.getName() + ".errorMessage";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CampusOneUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CampusOneUserDetailsService userDetailsService,
            RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = resolveBearerToken(request);
        if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AccessTokenClaims claims = jwtService.parseAccessToken(token);
            CampusOneUserPrincipal principal = userDetailsService.loadUserById(claims.userId());
            if (!jwtService.isAccessTokenValid(claims, principal)) {
                reject(request, response, "AUTH_INVALID_TOKEN", "The access token is invalid.");
                return;
            }

            UsernamePasswordAuthenticationToken authentication =
                    UsernamePasswordAuthenticationToken.authenticated(
                            principal,
                            null,
                            principal.getAuthorities());
            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException exception) {
            reject(request, response, "AUTH_TOKEN_EXPIRED", "The access token has expired.");
        } catch (JwtException | UsernameNotFoundException exception) {
            reject(request, response, "AUTH_INVALID_TOKEN", "The access token is invalid.");
        }
    }

    private String resolveBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response,
            String code,
            String message) throws IOException {
        SecurityContextHolder.clearContext();
        request.setAttribute(AUTH_ERROR_CODE, code);
        request.setAttribute(AUTH_ERROR_MESSAGE, message);
        authenticationEntryPoint.commence(
                request,
                response,
                new JwtAuthenticationException(message));
    }
}
