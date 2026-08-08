package com.app.fooddonation.service;

import com.app.fooddonation.event.DonationEvent;
import com.app.fooddonation.exception.InvalidStateTransitionException;
import com.app.fooddonation.model.Donation;
import com.app.fooddonation.model.DonationStatus;
import com.app.fooddonation.model.Role;
import com.app.fooddonation.model.User;
import com.app.fooddonation.repository.DonationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the donation state machine and race-free acceptance flow.
 */
@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private UserService userService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private DonationService donationService;

    private Donation pendingDonation() {
        Donation d = new Donation();
        d.setId(1L);
        d.setStatus(DonationStatus.PENDING);
        d.setFoodDescription("10 kg rice");
        d.setDonor(user(Role.DONOR, "donor@test.com"));
        d.setPickupTime(LocalDateTime.now().plusHours(2));
        return d;
    }

    private User user(Role role, String email) {
        User u = new User();
        u.setId(1L);
        u.setEmail(email);
        u.setRole(role);
        return u;
    }

    @Test
    @DisplayName("accepting a PENDING donation succeeds and publishes an event")
    void acceptDonation_whenPending_succeeds() {
        Donation donation = pendingDonation();
        when(donationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(donation));
        when(donationRepository.save(donation)).thenReturn(donation);
        when(userService.findByEmail("ngo@test.com")).thenReturn(user(Role.NGO, "ngo@test.com"));

        Donation result = donationService.acceptDonation(1L, "ngo@test.com");

        assertThat(result.getStatus()).isEqualTo(DonationStatus.ACCEPTED);
        assertThat(result.getNgo().getEmail()).isEqualTo("ngo@test.com");
        verify(donationRepository).save(donation);
        verify(eventPublisher).publishEvent(any(DonationEvent.class));
    }

    @Test
    @DisplayName("accepting a non-PENDING donation is rejected")
    void acceptDonation_whenAlreadyAccepted_throws() {
        Donation donation = pendingDonation();
        donation.setStatus(DonationStatus.ACCEPTED);
        when(donationRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(donation));
        when(userService.findByEmail("ngo@test.com")).thenReturn(user(Role.NGO, "ngo@test.com"));

        assertThatThrownBy(() -> donationService.acceptDonation(1L, "ngo@test.com"))
                .isInstanceOf(InvalidStateTransitionException.class);

        verify(donationRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("legal transition ACCEPTED -> PICKED_UP is allowed")
    void updateStatus_acceptedToPickedUp_succeeds() {
        Donation donation = pendingDonation();
        donation.setStatus(DonationStatus.ACCEPTED);
        when(donationRepository.findById(1L)).thenReturn(Optional.of(donation));
        when(donationRepository.save(donation)).thenReturn(donation);

        Donation result = donationService.updateDonationStatus(1L, DonationStatus.PICKED_UP);

        assertThat(result.getStatus()).isEqualTo(DonationStatus.PICKED_UP);
        verify(donationRepository).save(donation);
    }

    @Test
    @DisplayName("illegal transition ACCEPTED -> COMPLETED is rejected")
    void updateStatus_acceptedToCompleted_throws() {
        Donation donation = pendingDonation();
        donation.setStatus(DonationStatus.ACCEPTED);
        when(donationRepository.findById(1L)).thenReturn(Optional.of(donation));

        assertThatThrownBy(() -> donationService.updateDonationStatus(1L, DonationStatus.COMPLETED))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    @DisplayName("a completed donation cannot be cancelled")
    void cancelDonation_whenCompleted_throws() {
        Donation donation = pendingDonation();
        donation.setStatus(DonationStatus.COMPLETED);
        when(donationRepository.findById(1L)).thenReturn(Optional.of(donation));

        assertThatThrownBy(() -> donationService.cancelDonation(1L, "reason"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("expiring stale donations updates status and publishes events")
    void expireStaleDonations_expiresAndNotifies() {
        Donation d1 = pendingDonation();
        Donation d2 = pendingDonation();
        d2.setId(2L);
        when(donationRepository.findByStatusAndPickupTimeBefore(
                eq(DonationStatus.PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(d1, d2));
        when(donationRepository.save(any(Donation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        int expired = donationService.expireStaleDonations();

        assertThat(expired).isEqualTo(2);
        assertThat(d1.getStatus()).isEqualTo(DonationStatus.EXPIRED);
        assertThat(d2.getStatus()).isEqualTo(DonationStatus.EXPIRED);
        verify(donationRepository, times(2)).save(any(Donation.class));
        verify(eventPublisher, times(2)).publishEvent(any(DonationEvent.class));
    }

    @Test
    @DisplayName("full-text search degrades to the portable keyword search when native FTS is unavailable")
    void searchPendingDonations_fallsBackToKeywordSearch() {
        Page<Donation> results = new PageImpl<>(List.of(pendingDonation()));
        when(donationRepository.searchPendingFullText("rice", Pageable.unpaged()))
                .thenThrow(new RuntimeException("no full-text support"));
        when(donationRepository.searchPendingByKeyword(DonationStatus.PENDING, "rice", Pageable.unpaged()))
                .thenReturn(results);

        Page<Donation> page = donationService.searchPendingDonations("rice", Pageable.unpaged());

        assertThat(page.getContent()).hasSize(1);
        verify(donationRepository).searchPendingByKeyword(
                eq(DonationStatus.PENDING), eq("rice"), any(Pageable.class));
    }

    @Test
    @DisplayName("searching a new donation publishes a DonationCreatedEvent")
    void createDonation_publishesCreatedEvent() {
        when(userService.findByEmail("donor@test.com")).thenReturn(user(Role.DONOR, "donor@test.com"));
        when(donationRepository.save(any(Donation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        donationService.createDonation(new com.app.fooddonation.dto.DonationRequest(), "donor@test.com");

        verify(eventPublisher).publishEvent(any(com.app.fooddonation.event.DonationCreatedEvent.class));
    }
}
