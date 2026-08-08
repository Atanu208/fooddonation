package com.app.fooddonation.service;

import com.app.fooddonation.model.DonationStatus;
import com.app.fooddonation.model.Role;
import com.app.fooddonation.repository.DonationRepository;
import com.app.fooddonation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Aggregates platform-wide impact statistics. Results are cached in Redis (TTL 1 min)
 * and evicted automatically whenever donations or users change.
 */
@Service
public class StatsService {

    private static final Pattern QUANTITY_PATTERN = Pattern.compile("^\\s*(\\d+)");

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private UserRepository userRepository;

    @Cacheable(value = "platformStats", key = "'all'")
    public Map<String, Object> getPlatformStats() {
        long completed = donationRepository.countByStatus(DonationStatus.COMPLETED);
        long mealsServed = countMealsServed();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalDonations", donationRepository.count());
        stats.put("completedDonations", completed);
        stats.put("mealsServed", mealsServed);
        stats.put("wasteSavedKg", mealsServed / 2);
        stats.put("activeDonations",
                donationRepository.findAll().stream()
                        .filter(d -> d.getStatus() != DonationStatus.COMPLETED
                                && d.getStatus() != DonationStatus.CANCELLED)
                        .count());
        stats.put("totalDonors", userRepository.findByRole(Role.DONOR).size());
        stats.put("totalNGOs", userRepository.findByRole(Role.NGO).size());

        Map<String, Long> breakdown = new LinkedHashMap<>();
        for (DonationStatus status : DonationStatus.values()) {
            breakdown.put(status.name(), donationRepository.countByStatus(status));
        }
        stats.put("statusBreakdown", breakdown);
        return stats;
    }

    private long countMealsServed() {
        long meals = 0;
        for (var donation : donationRepository.findByStatus(DonationStatus.COMPLETED)) {
            meals += extractQuantity(donation.getQuantity());
        }
        return meals;
    }

    private long extractQuantity(String quantity) {
        if (quantity == null || quantity.isBlank()) {
            return 1;
        }
        Matcher matcher = QUANTITY_PATTERN.matcher(quantity.trim());
        return matcher.find() ? Long.parseLong(matcher.group(1)) : 1;
    }
}
