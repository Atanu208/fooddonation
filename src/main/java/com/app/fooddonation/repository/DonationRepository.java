package com.app.fooddonation.repository;

import com.app.fooddonation.model.Donation;
import com.app.fooddonation.model.DonationStatus;
import com.app.fooddonation.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByDonor(User donor);
    List<Donation> findByNgo(User ngo);
    List<Donation> findByStatus(DonationStatus status);
    List<Donation> findByDonorAndStatus(User donor, DonationStatus status);
    List<Donation> findByNgoAndStatus(User ngo, DonationStatus status);

    Page<Donation> findByDonor(User donor, Pageable pageable);
    Page<Donation> findByNgo(User ngo, Pageable pageable);
    Page<Donation> findByStatus(DonationStatus status, Pageable pageable);

    List<Donation> findByStatusAndPickupTimeBefore(DonationStatus status, LocalDateTime time);

    /**
     * Loads a donation with a pessimistic write lock so concurrent
     * NGOs cannot claim the same donation (race-free acceptance).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Donation d WHERE d.id = :id")
    Optional<Donation> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT d FROM Donation d WHERE d.status = :status AND d.pickupCity = :city")
    List<Donation> findPendingDonationsByCity(@Param("status") DonationStatus status,
                                              @Param("city") String city);

    @Query("SELECT d FROM Donation d WHERE d.status = :status AND d.pickupCity = :city")
    Page<Donation> findPendingDonationsByCity(@Param("status") DonationStatus status,
                                              @Param("city") String city, Pageable pageable);

    @Query("SELECT d FROM Donation d WHERE d.pickupTime BETWEEN :start AND :end")
    List<Donation> findDonationsBetweenDates(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    /**
     * PostgreSQL full-text search (backed by the {@code search_text} tsvector
     * column maintained by a DB trigger). PostgreSQL only - callers fall back
     * to {@link #searchPendingByKeyword} on other databases.
     */
    @Query(value = """
            SELECT d.* FROM donations d
            WHERE d.status = 'PENDING'
              AND d.search_text @@ plainto_tsquery('english', :q)
            ORDER BY d.created_at DESC
            """,
            countQuery = """
                    SELECT COUNT(d.id) FROM donations d
                    WHERE d.status = 'PENDING'
                      AND d.search_text @@ plainto_tsquery('english', :q)
                    """,
            nativeQuery = true)
    Page<Donation> searchPendingFullText(@Param("q") String q, Pageable pageable);

    /**
     * Portable keyword search over pending donations, used as a fallback on
     * non-PostgreSQL databases.
     */
    @Query("""
            SELECT d FROM Donation d
            WHERE d.status = :status
              AND (LOWER(d.foodDescription) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(d.pickupCity) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(d.pickupState) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(d.foodType) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY d.createdAt DESC
            """)
    Page<Donation> searchPendingByKeyword(@Param("status") DonationStatus status,
                                          @Param("q") String q, Pageable pageable);

    long countByStatus(DonationStatus status);
    long countByDonorAndStatus(User donor, DonationStatus status);
}