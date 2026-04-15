package com.donation.service;

import com.donation.model.Donation;
import com.donation.repository.DonationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class DonationService {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("1.00");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000.00");

    private final DonationRepository donationRepository;

    @Autowired
    public DonationService(DonationRepository donationRepository) {
        this.donationRepository = donationRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // CORE DONATION PROCESSING
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * Process a new donation: validates, assigns transaction ID, saves.
     */
    @Transactional
    public Donation processDonation(Donation donation) {
        // 1. Validate input
        validateDonation(donation);

        // 2. Set metadata
        donation.setStatus(Donation.DonationStatus.PENDING);
        donation.setDonationDate(LocalDateTime.now());
        donation.setTransactionId(generateTransactionId());

        // 3. Simulate payment gateway call
        boolean paymentSuccess = simulatePaymentGateway(donation);

        // 4. Update status based on payment result
        if (paymentSuccess) {
            donation.setStatus(Donation.DonationStatus.COMPLETED);
            donation.setRemarks("Payment processed successfully.");
        } else {
            donation.setStatus(Donation.DonationStatus.FAILED);
            donation.setRemarks("Payment gateway declined the transaction.");
        }

        // 5. Persist and return
        return donationRepository.save(donation);
    }

    /**
     * Retrieve a donation by ID.
     */
    public Optional<Donation> getDonationById(Long id) {
        return donationRepository.findById(id);
    }

    /**
     * Get all donations for a specific donor email.
     */
    public List<Donation> getDonationsByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank.");
        }
        return donationRepository.findByDonorEmail(email);
    }

    /**
     * Get all donations.
     */
    public List<Donation> getAllDonations() {
        return donationRepository.findAll();
    }

    /**
     * Get total amount of all completed donations.
     */
    public BigDecimal getTotalDonations() {
        BigDecimal total = donationRepository.getTotalCompletedDonations();
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Get donations filtered by cause.
     */
    public List<Donation> getDonationsByCause(String cause) {
        return donationRepository.findByCause(cause);
    }

    /**
     * Refund a completed donation.
     */
    @Transactional
    public Donation refundDonation(Long id) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Donation not found: " + id));

        if (donation.getStatus() != Donation.DonationStatus.COMPLETED) {
            throw new IllegalStateException("Only COMPLETED donations can be refunded.");
        }

        donation.setStatus(Donation.DonationStatus.REFUNDED);
        donation.setRemarks("Refund processed on " + LocalDateTime.now());
        return donationRepository.save(donation);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // VALIDATION LOGIC
    // ─────────────────────────────────────────────────────────────────────────────

    public void validateDonation(Donation donation) {
        if (donation == null) {
            throw new IllegalArgumentException("Donation cannot be null.");
        }

        if (donation.getDonorName() == null || donation.getDonorName().isBlank()) {
            throw new IllegalArgumentException("Donor name is required.");
        }

        if (donation.getDonorEmail() == null || !donation.getDonorEmail().matches("^[\\w.+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Invalid donor email.");
        }

        if (donation.getAmount() == null) {
            throw new IllegalArgumentException("Donation amount is required.");
        }

        if (donation.getAmount().compareTo(MIN_AMOUNT) < 0) {
            throw new IllegalArgumentException("Minimum donation amount is $1.00.");
        }

        if (donation.getAmount().compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException("Maximum donation amount is $100,000.");
        }

        if (donation.getPaymentMethod() == null || donation.getPaymentMethod().isBlank()) {
            throw new IllegalArgumentException("Payment method is required.");
        }

        List<String> validMethods = List.of("CREDIT_CARD", "DEBIT_CARD", "UPI", "NET_BANKING");
        if (!validMethods.contains(donation.getPaymentMethod().toUpperCase())) {
            throw new IllegalArgumentException("Invalid payment method. Allowed: " + validMethods);
        }

        if (donation.getCause() == null || donation.getCause().isBlank()) {
            throw new IllegalArgumentException("Cause is required.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────────────────────

    private String generateTransactionId() {
        return "TXN-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 12);
    }

    /**
     * Simulates a payment gateway — fails for amounts ending in .99 (test hook).
     */
    private boolean simulatePaymentGateway(Donation donation) {
        String amountStr = donation.getAmount().toPlainString();
        return !amountStr.endsWith(".99");
    }
}
