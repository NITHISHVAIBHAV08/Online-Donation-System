package com.donation.repository;

import com.donation.model.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    List<Donation> findByDonorEmail(String email);

    List<Donation> findByStatus(Donation.DonationStatus status);

    List<Donation> findByCause(String cause);

    @Query("SELECT SUM(d.amount) FROM Donation d WHERE d.status = 'COMPLETED'")
    BigDecimal getTotalCompletedDonations();

    @Query("SELECT d FROM Donation d WHERE d.amount >= :minAmount")
    List<Donation> findDonationsAboveAmount(BigDecimal minAmount);

    boolean existsByTransactionId(String transactionId);
}
