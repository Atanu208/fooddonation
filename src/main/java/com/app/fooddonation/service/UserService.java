package com.app.fooddonation.service;

import com.app.fooddonation.dto.UserRegistrationRequest;
import com.app.fooddonation.exception.DuplicateResourceException;
import com.app.fooddonation.exception.ResourceNotFoundException;
import com.app.fooddonation.model.Role;
import com.app.fooddonation.model.User;
import com.app.fooddonation.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(UserRegistrationRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setAddress(request.getAddress());
        user.setCity(request.getCity());
        user.setState(request.getState());
        user.setPincode(request.getPincode());
        user.setOrganizationName(request.getOrganizationName());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setActive(true);

        return userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> ResourceNotFoundException.user(email));
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public List<User> findAllNGOs() {
        return userRepository.findByRole(Role.NGO);
    }

    public Page<User> findAllNGOs(Pageable pageable) {
        return userRepository.findByRole(Role.NGO, pageable);
    }

    public Page<User> findAllNGOsByCity(String city, Pageable pageable) {
        return userRepository.findByCityAndRole(city, Role.NGO, pageable);
    }

    public List<User> findNGOsByCity(String city) {
        return userRepository.findByCityAndRole(city, Role.NGO);
    }

    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public User setActive(Long userId, boolean active) {
        User user = getById(userId);
        user.setActive(active);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(user);
    }

    public long countUsersByRole(Role role) {
        return userRepository.countByRole(role);
    }
}
