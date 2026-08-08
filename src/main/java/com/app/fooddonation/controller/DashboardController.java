package com.app.fooddonation.controller;

import com.app.fooddonation.model.Donation;
import com.app.fooddonation.model.DonationStatus;
import com.app.fooddonation.model.Role;
import com.app.fooddonation.model.User;
import com.app.fooddonation.service.DonationService;
import com.app.fooddonation.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    @Autowired
    private DonationService donationService;

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String homePage(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_DONOR") || a.getAuthority().equals("ROLE_NGO"))) {
            return "redirect:/dashboard";
        }
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        User user = userService.findByEmail(email);

        // Add user info
        model.addAttribute("user", user);

        // Role-specific dashboard
        if (user.getRole() == Role.DONOR) {
            return donorDashboard(user, model);
        } else if (user.getRole() == Role.NGO) {
            return ngoDashboard(user, model);
        } else if (user.getRole() == Role.ADMIN) {
            return "redirect:/admin";
        }

        return "redirect:/login";
    }

    private String donorDashboard(User donor, Model model) {
        // Get donor's donations
        List<Donation> donations = donationService.getDonationsByDonor(donor.getEmail());

        // Statistics
        long totalDonations = donations.size();
        long pendingDonations = donationService.countDonationsByDonorAndStatus(
                donor.getEmail(), DonationStatus.PENDING);
        long completedDonations = donationService.countDonationsByDonorAndStatus(
                donor.getEmail(), DonationStatus.COMPLETED);
        long cancelledDonations = donationService.countDonationsByDonorAndStatus(
                donor.getEmail(), DonationStatus.CANCELLED);

        // Recent donations
        List<Donation> recentDonations = donations.stream()
                .limit(5)
                .toList();

        model.addAttribute("totalDonations", totalDonations);
        model.addAttribute("pendingDonations", pendingDonations);
        model.addAttribute("completedDonations", completedDonations);
        model.addAttribute("cancelledDonations", cancelledDonations);
        model.addAttribute("recentDonations", recentDonations);
        model.addAttribute("donations", donations);

        return "donor/dashboard";
    }

    private String ngoDashboard(User ngo, Model model) {
        // Get NGO's claimed donations
        List<Donation> claimedDonations = donationService.getDonationsByNGO(ngo.getEmail());

        // Pending donations in their city
        List<Donation> pendingDonations = donationService.getPendingDonationsByCity(ngo.getCity());

        // Statistics
        long totalClaimed = claimedDonations.size();
        long pendingClaims = claimedDonations.stream()
                .filter(d -> d.getStatus() == DonationStatus.PENDING ||
                        d.getStatus() == DonationStatus.ACCEPTED)
                .count();
        long completedClaims = claimedDonations.stream()
                .filter(d -> d.getStatus() == DonationStatus.COMPLETED ||
                        d.getStatus() == DonationStatus.DELIVERED)
                .count();

        model.addAttribute("totalClaimed", totalClaimed);
        model.addAttribute("pendingClaims", pendingClaims);
        model.addAttribute("completedClaims", completedClaims);
        model.addAttribute("pendingDonations", pendingDonations);
        model.addAttribute("claimedDonations", claimedDonations);

        return "ngo/dashboard";
    }
}