package com.app.fooddonation.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.fooddonation.dto.ApiError;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Access denied handler for the form-login web chain. AJAX / JSON clients get
 * a structured JSON 403; browser requests are forwarded to the access-denied
 * page with a 403 status. Forwarding keeps the original HTTP method, so the
 * target mapping must accept all methods.
 */
@Component
public class WebAccessDeniedHandler implements AccessDeniedHandler {

    private static final String AJAX_HEADER = "XMLHttpRequest";

    private final ObjectMapper objectMapper;

    public WebAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        if (isJsonRequest(request)) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiError body = ApiError.of(HttpServletResponse.SC_FORBIDDEN, "Forbidden",
                    "FORBIDDEN", "You do not have permission to perform this action",
                    request.getRequestURI());
            objectMapper.writeValue(response.getOutputStream(), body);
            return;
        }

        request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpServletResponse.SC_FORBIDDEN);
        request.setAttribute(RequestDispatcher.ERROR_MESSAGE, accessDeniedException.getMessage());
        try {
            request.getRequestDispatcher("/access-denied").forward(request, response);
        } catch (ServletException e) {
            throw new IllegalStateException("Failed to forward to access-denied page", e);
        }
    }

    private boolean isJsonRequest(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return true;
        }
        return AJAX_HEADER.equalsIgnoreCase(request.getHeader("X-Requested-With"));
    }
}
