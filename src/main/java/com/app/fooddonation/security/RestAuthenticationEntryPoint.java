package com.app.fooddonation.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.fooddonation.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Returns a JSON 401 response for unauthenticated API requests instead of
 * redirecting to the HTML login page.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = ApiError.of(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized",
                "UNAUTHORIZED", "Authentication is required to access this resource",
                request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
