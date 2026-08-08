package com.app.fooddonation.controller.api;

import com.app.fooddonation.dto.AuthRequest;
import com.app.fooddonation.dto.AuthResponse;
import com.app.fooddonation.dto.UserDto;
import com.app.fooddonation.dto.UserRegistrationRequest;
import com.app.fooddonation.model.Role;
import com.app.fooddonation.model.User;
import com.app.fooddonation.security.JwtService;
import com.app.fooddonation.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public auth endpoints of the REST API. Returns a signed JWT on success.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, login and current-user endpoints")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtService jwtService;
    private final long jwtExpirationMs;

    public AuthApiController(AuthenticationManager authenticationManager,
                             UserService userService,
                             JwtService jwtService,
                             @Value("${app.jwt.expiration-ms:86400000}") long jwtExpirationMs) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.jwtService = jwtService;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user (DONOR or NGO)")
    public ResponseEntity<UserDto> register(@Valid @RequestBody UserRegistrationRequest request) {
        if (request.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Admin accounts cannot be self-registered");
        }
        User user = userService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserDto.from(user));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive a JWT")
    @com.app.fooddonation.ratelimit.RateLimit(bucket = "auth", capacity = 5, refillPeriodSeconds = 60)
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = (User) authentication.getPrincipal();
        String token = jwtService.generateToken(user);
        return ResponseEntity.ok(AuthResponse.of(token, jwtExpirationMs / 1000, user));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the currently authenticated user")
    public ResponseEntity<UserDto> me(Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(UserDto.from(user));
    }
}
