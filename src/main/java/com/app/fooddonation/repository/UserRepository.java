package com.app.fooddonation.repository;

import com.app.fooddonation.model.User;
import com.app.fooddonation.model.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByRole(Role role);
    Page<User> findByRole(Role role, Pageable pageable);
    List<User> findByCityAndRole(String city, Role role);
    Page<User> findByCityAndRole(String city, Role role, Pageable pageable);
    List<User> findByStateAndRole(String state, Role role);
    long countByRole(Role role);
}