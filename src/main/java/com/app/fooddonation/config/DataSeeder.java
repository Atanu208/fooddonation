package com.app.fooddonation.config;

import com.app.fooddonation.model.Donation;
import com.app.fooddonation.model.DonationStatus;
import com.app.fooddonation.model.Role;
import com.app.fooddonation.model.User;
import com.app.fooddonation.repository.DonationRepository;
import com.app.fooddonation.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DonationRepository donationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        log.info("Seeding demo data...");

        User donor1 = user("Rahul Sharma", "donor@demo.com", Role.DONOR,
                "9900112233", "Anand Vihar, Andheri East", "Mumbai",
                "Maharashtra", "400069", "Spice Garden Restaurant");
        User donor2 = user("Priya Patel", "donor2@demo.com", Role.DONOR,
                "9812345678", "Banjara Hills", "Hyderabad",
                "Telangana", "500034", null);
        User ngo1 = user("Aman Verma", "ngo@demo.com", Role.NGO,
                "9988776655", "Sion, Mumbai", "Mumbai",
                "Maharashtra", "400022", "Food for All Foundation");
        User ngo2 = user("Kavita Rao", "ngo2@demo.com", Role.NGO,
                "9555666777", "Kothapet", "Hyderabad",
                "Telangana", "500085", "Annapurna Seva Trust");
        User admin = user("Platform Admin", "admin@demo.com", Role.ADMIN,
                "9000000000", "HQ, Mumbai", "Mumbai",
                "Maharashtra", "400001", "FoodShare Platform");

        userRepository.saveAll(List.of(donor1, donor2, ngo1, ngo2, admin));

        donationRepository.save(donation(donor1, null,
                "20 boxes of vegetarian biryani (freshly cooked)", "20 boxes",
                "Spice Garden, Andheri East, Mumbai", "Mumbai", "Maharashtra", "400069",
                LocalDateTime.now().plusHours(3), DonationStatus.PENDING, null,
                LocalDateTime.now().minusMinutes(30), LocalDateTime.now().minusMinutes(30)));

        donationRepository.save(donation(donor1, null,
                "50 plates of chole bhature from lunch service", "50 plates",
                "Spice Garden, Andheri East, Mumbai", "Mumbai", "Maharashtra", "400069",
                LocalDateTime.now().plusHours(5), DonationStatus.PENDING, null,
                LocalDateTime.now().minusHours(1), LocalDateTime.now().minusHours(1)));

        donationRepository.save(donation(donor2, null,
                "30 kg of leftover cooked rice and dal", "30 kg",
                "Taj Mahal Banquet, Banjara Hills, Hyderabad", "Hyderabad", "Telangana", "500034",
                LocalDateTime.now().plusHours(2), DonationStatus.PENDING, null,
                LocalDateTime.now().minusMinutes(45), LocalDateTime.now().minusMinutes(45)));

        LocalDateTime acceptedAt = LocalDateTime.now().minusDays(1).minusHours(2);
        donationRepository.save(donation(donor1, ngo1,
                "15 packets of idli-sambar breakfast boxes", "15 packets",
                "Spice Garden, Andheri East, Mumbai", "Mumbai", "Maharashtra", "400069",
                acceptedAt.plusHours(1), DonationStatus.ACCEPTED, null,
                acceptedAt, acceptedAt.plusHours(1)));

        LocalDateTime completedAt = LocalDateTime.now().minusDays(2);
        donationRepository.save(donation(donor2, ngo2,
                "25 kg of khichdi from a community kitchen", "25 kg",
                "Community Hall, Hyderabad", "Hyderabad", "Telangana", "500034",
                completedAt.minusHours(2), DonationStatus.COMPLETED, completedAt,
                completedAt.minusHours(3), completedAt));

        log.info("Demo data seeded. Donor: donor@demo.com / NGO: ngo@demo.com / Admin: admin@demo.com (password: password123)");
    }

    private Donation donation(User donor, User ngo, String foodDescription, String quantity,
                              String address, String city, String state, String pincode,
                              LocalDateTime pickupTime, DonationStatus status,
                              LocalDateTime completedAt, LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
        Donation donation = new Donation();
        donation.setDonor(donor);
        donation.setNgo(ngo);
        donation.setFoodDescription(foodDescription);
        donation.setQuantity(quantity);
        donation.setFoodType("Vegetarian");
        donation.setPickupAddress(address);
        donation.setPickupCity(city);
        donation.setPickupState(state);
        donation.setPickupPincode(pincode);
        donation.setPickupTime(pickupTime);
        donation.setExpiryTime("Within 2 hours");
        donation.setPackaged(true);
        donation.setStatus(status);
        donation.setCompletedAt(completedAt);
        donation.setCreatedAt(createdAt);
        donation.setUpdatedAt(updatedAt);
        return donation;
    }

    private User user(String name, String email, Role role, String phone,
                      String address, String city, String state, String pincode,
                      String organizationName) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setPhoneNumber(phone);
        user.setAddress(address);
        user.setCity(city);
        user.setState(state);
        user.setPincode(pincode);
        user.setOrganizationName(organizationName);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);
        return user;
    }
}
