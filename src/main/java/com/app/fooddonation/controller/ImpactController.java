package com.app.fooddonation.controller;

import com.app.fooddonation.model.Donation;
import com.app.fooddonation.model.DonationStatus;
import com.app.fooddonation.model.Role;
import com.app.fooddonation.service.DonationService;
import com.app.fooddonation.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ImpactController {

    @Autowired
    private DonationService donationService;

    @Autowired
    private UserService userService;

    @GetMapping("/impact")
    public String impact(Authentication authentication, Model model) {
        long totalDonations = donationService.countAll();
        long completedDonations = donationService.countDonationsByStatus(DonationStatus.COMPLETED);
        long mealsServed = donationService.countMealsServed();
        long wasteSavedKg = mealsServed / 2;

        model.addAttribute("totalDonations", totalDonations);
        model.addAttribute("completedDonations", completedDonations);
        model.addAttribute("mealsServed", mealsServed);
        model.addAttribute("wasteSavedKg", wasteSavedKg);
        model.addAttribute("activeDonations",
                donationService.countDonationsByStatuses(List.of(
                        DonationStatus.PENDING, DonationStatus.ACCEPTED,
                        DonationStatus.PICKED_UP, DonationStatus.DELIVERED)));
        model.addAttribute("totalDonors", userService.countUsersByRole(Role.DONOR));
        model.addAttribute("totalNGOs", userService.countUsersByRole(Role.NGO));

        List<Donation> recentCompleted = donationService.getCompletedDonations().stream()
                .sorted((d1, d2) -> d2.getCompletedAt() == null ? 1 : d2.getCompletedAt()
                        .compareTo(d1.getCompletedAt() == null ? d1.getCreatedAt() : d1.getCompletedAt()))
                .limit(5)
                .toList();
        model.addAttribute("recentCompleted", recentCompleted);

        return "impact";
    }
}
