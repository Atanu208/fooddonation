package com.app.fooddonation.controller;

import com.app.fooddonation.model.Donation;
import com.app.fooddonation.model.User;
import com.app.fooddonation.service.DonationService;
import com.app.fooddonation.service.StatsService;
import com.app.fooddonation.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private DonationService donationService;

    @Autowired
    private StatsService statsService;

    @GetMapping("/admin")
    public String adminDashboard(Authentication authentication, Model model) {
        User admin = userService.findByEmail(authentication.getName());
        model.addAttribute("admin", admin);
        model.addAttribute("stats", statsService.getPlatformStats());
        return "admin/dashboard";
    }

    @GetMapping("/admin/users")
    public String users(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<User> users = userService.findAllUsers(
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
        model.addAttribute("users", users);
        return "admin/users";
    }

    @GetMapping("/admin/donations")
    public String donations(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<Donation> donations = donationService.getAllDonations(
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
        model.addAttribute("donations", donations);
        return "admin/donations";
    }

    @PostMapping("/admin/users/{id}/toggle-active")
    public String toggleActive(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User user = userService.getById(id);
        userService.setActive(id, !user.isActive());
        redirectAttributes.addFlashAttribute("success",
                "User " + user.getEmail() + " " + (user.isActive() ? "deactivated" : "activated"));
        return "redirect:/admin/users";
    }
}
