package com.app.fooddonation.controller;

import com.app.fooddonation.dto.DonationRequest;
import com.app.fooddonation.model.Donation;
import com.app.fooddonation.model.DonationStatus;
import com.app.fooddonation.model.Role;
import com.app.fooddonation.model.User;
import com.app.fooddonation.service.DonationService;
import com.app.fooddonation.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class DonationController {

    @Autowired
    private DonationService donationService;

    @Autowired
    private UserService userService;

    // Donor: Create donation
    @GetMapping("/donor/create-donation")
    public String showCreateDonationForm(Model model, Authentication authentication) {
        if (!model.containsAttribute("donation")) {
            model.addAttribute("donation", new DonationRequest());
        }

        User donor = userService.findByEmail(authentication.getName());
        model.addAttribute("donor", donor);

        return "donor/create-donation";
    }

    @PostMapping("/donor/create-donation")
    public String createDonation(@Valid @ModelAttribute("donation") DonationRequest request,
                                 BindingResult result,
                                 Authentication authentication,
                                 Model model,
                                 RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "donor/create-donation";
        }

        try {
            Donation donation = donationService.createDonation(request, authentication.getName());
            redirectAttributes.addFlashAttribute("success",
                    "Donation created successfully! ID: " + donation.getId());
            return "redirect:/donor/my-donations";

        } catch (Exception e) {
            model.addAttribute("error", "Failed to create donation: " + e.getMessage());
            return "donor/create-donation";
        }
    }

    // Donor: View my donations
    @GetMapping("/donor/my-donations")
    public String viewMyDonations(Authentication authentication, Model model) {
        List<Donation> donations = donationService.getDonationsByDonor(authentication.getName());
        model.addAttribute("donations", donations);
        model.addAttribute("donor", userService.findByEmail(authentication.getName()));
        return "donor/my-donations";
    }

    // NGO: View available donations
    @GetMapping("/ngo/available-donations")
    public String viewAvailableDonations(@RequestParam(required = false) String city,
                                         @RequestParam(required = false) String q,
                                         Authentication authentication, Model model) {
        User ngo = userService.findByEmail(authentication.getName());
        String searchCity = (city != null && !city.isBlank()) ? city : ngo.getCity();

        List<Donation> cityDonations;
        List<Donation> allPendingDonations;

        if (q != null && !q.isBlank()) {
            List<Donation> matches = donationService
                    .searchPendingDonations(q, PageRequest.of(0, 100))
                    .getContent();
            cityDonations = matches.stream()
                    .filter(d -> searchCity != null
                            && searchCity.equalsIgnoreCase(d.getPickupCity()))
                    .toList();
            allPendingDonations = matches.stream()
                    .filter(d -> !cityDonations.contains(d))
                    .toList();
        } else {
            // Show pending donations in NGO's city first
            cityDonations = donationService.getPendingDonationsByCity(searchCity);
            allPendingDonations = donationService.getPendingDonations();

            // Filter out city donations from all to avoid duplicates
            allPendingDonations.removeAll(cityDonations);
        }

        model.addAttribute("cityDonations", cityDonations);
        model.addAttribute("allPendingDonations", allPendingDonations);
        model.addAttribute("ngo", ngo);
        model.addAttribute("filterCity", searchCity);
        model.addAttribute("searchQuery", q);

        return "ngo/available-donations";
    }

    // NGO: Accept donation
    @PostMapping("/ngo/accept/{donationId}")
    public String acceptDonation(@PathVariable Long donationId,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            Donation donation = donationService.acceptDonation(donationId, authentication.getName());
            redirectAttributes.addFlashAttribute("success",
                    "Donation accepted successfully! Please arrange for pickup.");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to accept donation: " + e.getMessage());
        }

        return "redirect:/ngo/available-donations";
    }

    // NGO: View my claims
    @GetMapping("/ngo/my-claims")
    public String viewMyClaims(Authentication authentication, Model model) {
        List<Donation> donations = donationService.getDonationsByNGO(authentication.getName());
        model.addAttribute("donations", donations);
        model.addAttribute("ngo", userService.findByEmail(authentication.getName()));
        return "ngo/my-claims";
    }

    // Update donation status (for both donor and NGO)
    @PostMapping("/donation/update-status/{donationId}")
    public String updateDonationStatus(@PathVariable Long donationId,
                                       @RequestParam DonationStatus status,
                                       @RequestParam(required = false) String reason,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        try {
            Donation donation = donationService.updateDonationStatus(donationId, status);
            String role = userService.findByEmail(authentication.getName()).getRole().name();
            redirectAttributes.addFlashAttribute("success",
                    "Donation status updated to: " + status.getDisplayName());

            return "redirect:/" + role.toLowerCase() + "/my-" +
                    (role.equals("DONOR") ? "donations" : "claims");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to update status: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    // Cancel donation
    @PostMapping("/donation/cancel/{donationId}")
    public String cancelDonation(@PathVariable Long donationId,
                                 @RequestParam String reason,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        try {
            donationService.cancelDonation(donationId, reason);
            String role = userService.findByEmail(authentication.getName()).getRole().name();
            redirectAttributes.addFlashAttribute("success", "Donation cancelled successfully");

            return "redirect:/" + role.toLowerCase() + "/my-" +
                    (role.equals("DONOR") ? "donations" : "claims");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to cancel donation: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    // View donation details
    @GetMapping("/donation/{id}")
    public String viewDonationDetails(@PathVariable Long id, Model model) {
        Donation donation = donationService.getDonationById(id);
        model.addAttribute("donation", donation);
        return "donation/details";
    }

    // Public: About page
    @GetMapping("/about")
    public String aboutPage() {
        return "about";
    }

    @GetMapping("/contact")
    public String contactPage() {
        return "contact";
    }

    @PostMapping("/contact")
    public String submitContact(@RequestParam String name,
                                @RequestParam String email,
                                @RequestParam(required = false) String subject,
                                @RequestParam String message,
                                RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("success",
                "Thank you, " + name + "! Your message has been received. We'll get back to you soon.");
        return "redirect:/contact";
    }
}