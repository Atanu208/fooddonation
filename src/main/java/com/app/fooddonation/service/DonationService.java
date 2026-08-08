package com.app.fooddonation.service;

import com.app.fooddonation.dto.DonationRequest;
import com.app.fooddonation.event.DonationCreatedEvent;
import com.app.fooddonation.event.DonationEvent;
import com.app.fooddonation.exception.InvalidStateTransitionException;
import com.app.fooddonation.exception.ResourceNotFoundException;
import com.app.fooddonation.model.Donation;
import com.app.fooddonation.model.DonationStatus;
import com.app.fooddonation.model.User;
import com.app.fooddonation.repository.DonationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DonationService {

    private static final Logger log = LoggerFactory.getLogger(DonationService.class);
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("^\\s*(\\d+)");

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @CacheEvict(cacheNames = "platformStats", allEntries = true)
    @Transactional
    public Donation createDonation(DonationRequest request, String donorEmail) {
        User donor = userService.findByEmail(donorEmail);

        Donation donation = new Donation();
        donation.setDonor(donor);
        donation.setFoodDescription(request.getFoodDescription());
        donation.setPickupAddress(request.getPickupAddress());
        donation.setPickupCity(request.getPickupCity());
        donation.setPickupState(request.getPickupState());
        donation.setPickupPincode(request.getPickupPincode());
        donation.setPickupTime(request.getPickupTime());
        donation.setQuantity(request.getQuantity());
        donation.setFoodType(request.getFoodType());
        donation.setPackaged(request.isPackaged());
        donation.setExpiryTime(request.getExpiryTime());
        donation.setSpecialInstructions(request.getSpecialInstructions());
        donation.setStatus(DonationStatus.PENDING);
        donation.setCreatedAt(LocalDateTime.now());
        donation.setUpdatedAt(LocalDateTime.now());

        Donation saved = donationRepository.save(donation);
        eventPublisher.publishEvent(new DonationCreatedEvent(saved));
        log.info("Donation {} created by {}", saved.getId(), donorEmail);
        return saved;
    }

    public List<Donation> getDonationsByDonor(String donorEmail) {
        User donor = userService.findByEmail(donorEmail);
        return donationRepository.findByDonor(donor);
    }

    public Page<Donation> getDonationsByDonor(String donorEmail, Pageable pageable) {
        User donor = userService.findByEmail(donorEmail);
        return donationRepository.findByDonor(donor, pageable);
    }

    public List<Donation> getDonationsByNGO(String ngoEmail) {
        User ngo = userService.findByEmail(ngoEmail);
        return donationRepository.findByNgo(ngo);
    }

    public Page<Donation> getDonationsByNGO(String ngoEmail, Pageable pageable) {
        User ngo = userService.findByEmail(ngoEmail);
        return donationRepository.findByNgo(ngo, pageable);
    }

    public List<Donation> getPendingDonations() {
        return donationRepository.findByStatus(DonationStatus.PENDING);
    }

    public Page<Donation> getPendingDonations(Pageable pageable) {
        return donationRepository.findByStatus(DonationStatus.PENDING, pageable);
    }

    public List<Donation> getPendingDonationsByCity(String city) {
        return donationRepository.findPendingDonationsByCity(DonationStatus.PENDING, city);
    }

    public Page<Donation> getPendingDonationsByCity(String city, Pageable pageable) {
        return donationRepository.findPendingDonationsByCity(DonationStatus.PENDING, city, pageable);
    }

    /**
     * Full-text search over PENDING donations. Uses PostgreSQL's native
     * tsvector/plainto_tsquery search; degrades to an ILIKE keyword search on
     * databases that lack full-text support (e.g. H2 during tests).
     */
    public Page<Donation> searchPendingDonations(String query, Pageable pageable) {
        try {
            return donationRepository.searchPendingFullText(query, pageable);
        } catch (Exception ex) {
            return donationRepository.searchPendingByKeyword(DonationStatus.PENDING, query, pageable);
        }
    }

    public Page<Donation> getAllDonations(Pageable pageable) {
        return donationRepository.findAll(pageable);
    }

    public Donation getDonationById(Long id) {
        return donationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.donation(id));
    }

    @Transactional
    public Donation acceptDonation(Long donationId, String ngoEmail) {
        // Pessimistic write lock prevents two NGOs from claiming the same donation.
        Donation donation = donationRepository.findByIdForUpdate(donationId)
                .orElseThrow(() -> ResourceNotFoundException.donation(donationId));
        User ngo = userService.findByEmail(ngoEmail);

        if (donation.getStatus() != DonationStatus.PENDING) {
            throw new InvalidStateTransitionException(
                    "Donation is not available - current status: " + donation.getStatus().getDisplayName());
        }

        donation.setNgo(ngo);
        donation.setStatus(DonationStatus.ACCEPTED);
        donation.setUpdatedAt(LocalDateTime.now());

        Donation saved = donationRepository.save(donation);
        eventPublisher.publishEvent(DonationEvent.of(saved, DonationStatus.ACCEPTED, ngoEmail));
        log.info("Donation {} accepted by NGO {}", donationId, ngoEmail);
        return saved;
    }

    @Transactional
    public Donation updateDonationStatus(Long donationId, DonationStatus status) {
        Donation donation = getDonationById(donationId);
        DonationStatus current = donation.getStatus();

        if (!canTransition(current, status)) {
            throw InvalidStateTransitionException.between(current, status);
        }

        if (current == status) {
            throw new IllegalStateException("Donation is already in " + status.getDisplayName() + " state");
        }

        donation.setStatus(status);
        donation.setUpdatedAt(LocalDateTime.now());

        if (status == DonationStatus.COMPLETED) {
            donation.setCompletedAt(LocalDateTime.now());
        }

        Donation saved = donationRepository.save(donation);
        eventPublisher.publishEvent(DonationEvent.of(saved, status, null));
        return saved;
    }

    @Transactional
    public Donation cancelDonation(Long donationId, String reason) {
        Donation donation = getDonationById(donationId);

        if (donation.getStatus() == DonationStatus.COMPLETED ||
                donation.getStatus() == DonationStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel a completed donation");
        }

        donation.setStatus(DonationStatus.CANCELLED);
        donation.setSpecialInstructions(reason != null ? reason : "Cancelled by user");
        donation.setUpdatedAt(LocalDateTime.now());

        Donation saved = donationRepository.save(donation);
        eventPublisher.publishEvent(DonationEvent.of(saved, DonationStatus.CANCELLED, null, reason));
        return saved;
    }

    /**
     * Marks PENDING donations whose pickup time has passed as EXPIRED.
     * Runs periodically from a scheduled job.
     */
    @Transactional
    public int expireStaleDonations() {
        List<Donation> stale = donationRepository
                .findByStatusAndPickupTimeBefore(DonationStatus.PENDING, LocalDateTime.now());
        int count = 0;
        for (Donation donation : stale) {
            donation.setStatus(DonationStatus.EXPIRED);
            donation.setUpdatedAt(LocalDateTime.now());
            Donation saved = donationRepository.save(donation);
            eventPublisher.publishEvent(DonationEvent.of(saved, DonationStatus.EXPIRED, "system"));
            count++;
        }
        if (count > 0) {
            log.info("Expired {} stale donation(s)", count);
        }
        return count;
    }

    private boolean canTransition(DonationStatus from, DonationStatus to) {
        return switch (from) {
            case ACCEPTED -> to == DonationStatus.PICKED_UP;
            case PICKED_UP -> to == DonationStatus.DELIVERED;
            case DELIVERED -> to == DonationStatus.COMPLETED;
            default -> false;
        };
    }

    public long countDonationsByStatus(DonationStatus status) {
        return donationRepository.countByStatus(status);
    }

    public long countDonationsByDonorAndStatus(String donorEmail, DonationStatus status) {
        User donor = userService.findByEmail(donorEmail);
        return donationRepository.countByDonorAndStatus(donor, status);
    }

    public List<Donation> getRecentDonations(int limit) {
        return donationRepository.findAll().stream()
                .sorted((d1, d2) -> d2.getCreatedAt().compareTo(d1.getCreatedAt()))
                .limit(limit)
                .toList();
    }

    public List<Donation> getCompletedDonations() {
        return donationRepository.findByStatus(DonationStatus.COMPLETED);
    }

    public long countAll() {
        return donationRepository.count();
    }

    public long countDonationsByStatuses(List<DonationStatus> statuses) {
        return donationRepository.findAll().stream()
                .filter(d -> statuses.contains(d.getStatus()))
                .count();
    }

    public long countMealsServed() {
        long meals = 0;
        for (Donation d : getCompletedDonations()) {
            meals += extractQuantity(d.getQuantity());
        }
        return meals;
    }

    public long extractQuantity(String quantity) {
        if (quantity == null || quantity.isBlank()) {
            return 1;
        }
        Matcher matcher = QUANTITY_PATTERN.matcher(quantity.trim());
        return matcher.find() ? Long.parseLong(matcher.group(1)) : 1;
    }
}
