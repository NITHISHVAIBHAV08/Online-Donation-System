package com.donation.controller;

import com.donation.model.Donation;
import com.donation.service.DonationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/donations")
public class DonationController {

    private final DonationService donationService;

    @Autowired
    public DonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    // POST /api/donations — process a new donation
    @PostMapping
    public ResponseEntity<?> createDonation(@Valid @RequestBody Donation donation) {
        try {
            Donation processed = donationService.processDonation(donation);
            return ResponseEntity.status(HttpStatus.CREATED).body(processed);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /api/donations — get all donations
    @GetMapping
    public ResponseEntity<List<Donation>> getAllDonations() {
        return ResponseEntity.ok(donationService.getAllDonations());
    }

    // GET /api/donations/{id} — get donation by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getDonationById(@PathVariable Long id) {
        return donationService.getDonationById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Donation not found with id: " + id)));
    }

    // GET /api/donations/email/{email} — donations by donor email
    @GetMapping("/email/{email}")
    public ResponseEntity<List<Donation>> getDonationsByEmail(@PathVariable String email) {
        return ResponseEntity.ok(donationService.getDonationsByEmail(email));
    }

    // GET /api/donations/cause/{cause} — donations by cause
    @GetMapping("/cause/{cause}")
    public ResponseEntity<List<Donation>> getDonationsByCause(@PathVariable String cause) {
        return ResponseEntity.ok(donationService.getDonationsByCause(cause));
    }

    // GET /api/donations/total — total completed amount
    @GetMapping("/total")
    public ResponseEntity<Map<String, BigDecimal>> getTotalDonations() {
        BigDecimal total = donationService.getTotalDonations();
        return ResponseEntity.ok(Map.of("totalDonations", total));
    }

    // PUT /api/donations/{id}/refund — refund a donation
    @PutMapping("/{id}/refund")
    public ResponseEntity<?> refundDonation(@PathVariable Long id) {
        try {
            Donation refunded = donationService.refundDonation(id);
            return ResponseEntity.ok(refunded);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
