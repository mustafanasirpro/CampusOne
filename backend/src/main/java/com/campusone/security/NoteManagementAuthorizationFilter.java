package com.campusone.security;

import com.campusone.note.service.NoteAdminAuthorizationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class NoteManagementAuthorizationFilter
        extends OncePerRequestFilter {

    private static final String NOTES_PATH = "/api/v1/notes";
    private static final String UPLOAD_PATH = "/api/v1/notes/upload";

    private final NoteAdminAuthorizationService authorizationService;
    private final SecurityErrorResponseWriter errorResponseWriter;

    public NoteManagementAuthorizationFilter(
            NoteAdminAuthorizationService authorizationService,
            SecurityErrorResponseWriter errorResponseWriter) {
        this.authorizationService = authorizationService;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getServletPath();
        if (HttpMethod.POST.matches(method)) {
            return !NOTES_PATH.equals(path) && !UPLOAD_PATH.equals(path);
        }
        if (HttpMethod.PATCH.matches(method)
                || HttpMethod.DELETE.matches(method)) {
            return !isNoteDetailPath(path);
        }
        return true;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (authentication.getPrincipal()
                instanceof CampusOneUserPrincipal principal
                && authorizationService.canManage(
                        principal.getUserId(),
                        principal.getUsername())) {
            filterChain.doFilter(request, response);
            return;
        }

        errorResponseWriter.write(
                request,
                response,
                HttpStatus.FORBIDDEN.value(),
                HttpStatus.FORBIDDEN.getReasonPhrase(),
                "NOTE_ADMIN_REQUIRED",
                "Only admins can upload or manage notes.");
    }

    private boolean isNoteDetailPath(String path) {
        if (!path.startsWith(NOTES_PATH + "/")) {
            return false;
        }
        String remainder = path.substring((NOTES_PATH + "/").length());
        return !remainder.isBlank() && !remainder.contains("/");
    }
}
