package com.app.fooddonation.controller.api;

import com.app.fooddonation.dto.DonationRequest;
import com.app.fooddonation.dto.DonationResponse;
import com.app.fooddonation.dto.MessageResponse;
import com.app.fooddonation.exception.ResourceNotFoundException;
import com.app.fooddonation.exception.UnauthorizedActionException;
import com.app.fooddonation.model.Donation;
import com.app.fooddonation.model.DonationStatus;
import com.app.fooddonation.model.Role;
import com.app.fooddonation.model.User;
import com.app.fooddonation.service.DonationService;
import com.app.fooddonation.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Donations", description = "Create, claim and manage food donations")
public class DonationApiController {

    private final DonationService donationService;
    private final UserService userService;

    public DonationApiController(DonationService donationService, UserService userService) {
        this.donationService = donationService;
        this.userService = userService;
    }

    @GetMapping("/donations")
    @Operation(summary = "List donations (role-aware)")
    public ResponseEntity<Page<DonationResponse>> listDonations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String city,
            Authentication authentication) {

        Pageable pageable = buildPageable(page, size, sortBy);
        User user = userService.findByEmail(authentication.getName());

        Page<Donation> result;
        switch (user.getRole()) {
            case DONOR -> result = donationService.getDonationsByDonor(user.getEmail(), pageable);
            case NGO -> result = city != null && !city.isBlank()
                    ? donationService.getPendingDonationsByCity(city, pageable)
                    : donationService.getPendingDonations(pageable);
            case ADMIN -> result = donationService.getAllDonations(pageable);
            default -> throw new UnauthorizedActionException("Unsupported role");
        }
        return ResponseEntity.ok(result.map(DonationResponse::from));
    }

    @GetMapping("/donations/mine")
    @Operation(summary = "List the current user's own donations / claims")
    public ResponseEntity<Page<DonationResponse>> myDonations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Pageable pageable = buildPageable(page, size, "createdAt");
        User user = userService.findByEmail(authentication.getName());
        Page<Donation> result = user.getRole() == Role.DONOR
                ? donationService.getDonationsByDonor(user.getEmail(), pageable)
                : donationService.getDonationsByNGO(user.getEmail(), pageable);
        return ResponseEntity.ok(result.map(DonationResponse::from));
    }

    @GetMapping("/donations/{id}")
    @Operation(summary = "Get a single donation")
    public ResponseEntity<DonationResponse> getDonation(@PathVariable Long id) {
        return ResponseEntity.ok(DonationResponse.from(donationService.getDonationById(id)));
    }

    @PostMapping("/donations")
    @Operation(summary = "Create a donation (DONOR)")
    public ResponseEntity<DonationResponse> createDonation(
            @Valid @RequestBody DonationRequest request,
            Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        if (user.getRole() != Role.DONOR) {
            throw new UnauthorizedActionException("Only DONOR users can create donations");
        }
        Donation donation = donationService.createDonation(request, user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(DonationResponse.from(donation));
    }

    @PostMapping("/donations/{id}/accept")
    @Operation(summary = "Accept a pending donation (NGO)")
    public ResponseEntity<DonationResponse> acceptDonation(@PathVariable Long id,
                                                           Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        if (user.getRole() != Role.NGO) {
            throw new UnauthorizedActionException("Only NGO users can accept donations");
        }
        Donation donation = donationService.acceptDonation(id, user.getEmail());
        return ResponseEntity.ok(DonationResponse.from(donation));
    }

    @PutMapping("/donations/{id}/status")
    @Operation(summary = "Transition donation status (PICKED_UP -> DELIVERED -> COMPLETED)")
    public ResponseEntity<DonationResponse> updateStatus(@PathVariable Long id,
                                                         @RequestParam DonationStatus status,
                                                         Authentication authentication) {
        User user = userService.findByEmail(authentication.getName());
        if (user.getRole() == Role.ADMIN) {
            throw new UnauthorizedActionException("Admins cannot change donation status directly");
        }
        Donation donation = donationService.updateDonationStatus(id, status);
        return ResponseEntity.ok(DonationResponse.from(donation));
    }

    @PostMapping("/donations/{id}/cancel")
    @Operation(summary = "Cancel a donation")
    public ResponseEntity<DonationResponse> cancelDonation(@PathVariable Long id,
                                                           @RequestParam(required = false) String reason,
                                                           Authentication authentication) {
        Donation donation = donationService.cancelDonation(id, reason);
        return ResponseEntity.ok(DonationResponse.from(donation));
    }

    @GetMapping("/ngos")
    @Operation(summary = "List registered NGOs")
    public ResponseEntity<Page<com.app.fooddonation.dto.UserDto>> listNgos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String city) {
        Pageable pageable = buildPageable(page, size, null);
        Page<com.app.fooddonation.model.User> result;
        if (city != null && !city.isBlank()) {
            result = userService.findAllNGOsByCity(city, pageable);
        } else {
            result = userService.findAllNGOs(pageable);
        }
        return ResponseEntity.ok(result.map(com.app.fooddonation.dto.UserDto::from));
    }

    private Pageable buildPageable(int page, int size, String sortBy) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }
        String safeSort = switch (sortBy) {
            case "createdAt", "pickupTime", "status" -> sortBy;
            default -> "createdAt";
        };
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, safeSort));
    }
}
