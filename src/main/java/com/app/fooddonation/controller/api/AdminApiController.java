package com.app.fooddonation.controller.api;

import com.app.fooddonation.dto.DonationResponse;
import com.app.fooddonation.dto.MessageResponse;
import com.app.fooddonation.dto.UserDto;
import com.app.fooddonation.service.DonationService;
import com.app.fooddonation.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only management endpoints (users + platform overview).
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Administrative management endpoints (ADMIN only)")
public class AdminApiController {

    private final UserService userService;
    private final DonationService donationService;

    public AdminApiController(UserService userService, DonationService donationService) {
        this.userService = userService;
        this.donationService = donationService;
    }

    @GetMapping("/users")
    @Operation(summary = "List all users")
    public ResponseEntity<Page<UserDto>> listUsers(@RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(userService.findAllUsers(pageable).map(UserDto::from));
    }

    @GetMapping("/donations")
    @Operation(summary = "List all donations")
    public ResponseEntity<Page<DonationResponse>> listDonations(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(donationService.getAllDonations(pageable).map(DonationResponse::from));
    }

    @PatchMapping("/users/{id}/active")
    @Operation(summary = "Activate or deactivate a user account")
    public ResponseEntity<UserDto> setActive(@PathVariable Long id,
                                             @RequestParam boolean active) {
        return ResponseEntity.ok(UserDto.from(userService.setActive(id, active)));
    }

    @GetMapping("/ping")
    @Operation(summary = "Admin endpoint availability check")
    public ResponseEntity<MessageResponse> ping() {
        return ResponseEntity.ok(MessageResponse.of("pong"));
    }
}
