package com.campusconnect.security;

import com.campusconnect.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** Makes 401 / 403 responses from the security layer use the same JSON error shape. */
@Component
public class JsonSecurityHandlers {

    private final ObjectMapper objectMapper;

    public JsonSecurityHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuthenticationEntryPoint entryPoint() {
        return (request, response, ex) -> {
            Object reason = request.getAttribute(JwtAuthFilter.AUTH_ERROR_ATTRIBUTE);
            write(request, response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED",
                    reason != null ? reason.toString() : "Please log in to continue.");
        };
    }

    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) -> write(request, response, HttpServletResponse.SC_FORBIDDEN,
                "FORBIDDEN", "You don't have permission to do this.");
    }

    private void write(HttpServletRequest req, HttpServletResponse res, int status, String code, String message)
            throws IOException {
        res.setStatus(status);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getOutputStream(), ErrorResponse.of(code, message, req.getRequestURI()));
    }
}
