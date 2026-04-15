package com.donation.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "donations")
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Donor name is required")
    @Column(nullable = false)
    private String donorName;

    @NotBlank(message = "Donor email is required")
    @Email(message = "Invalid email format")
    @Column(nullable = false)
    private String donorEmail;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Minimum donation is $1.00")
    @DecimalMax(value = "100000.00", message = "Maximum donation is $100,000")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @NotBlank(message = "Payment method is required")
    @Column(nullable = false)
    private String paymentMethod; // CREDIT_CARD, DEBIT_CARD, UPI, NET_BANKING

    @Column(nullable = false)
    private String cause; // EDUCATION, HEALTH, ENVIRONMENT, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DonationStatus status;

    @Column(nullable = false)
    private LocalDateTime donationDate;

    private String transactionId;

    private String remarks;

    public enum DonationStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }

    // ─── Constructors ────────────────────────────────────────────────────────────

    public Donation() {}

    public Donation(String donorName, String donorEmail, BigDecimal amount,
                    String paymentMethod, String cause) {
        this.donorName     = donorName;
        this.donorEmail    = donorEmail;
        this.amount        = amount;
        this.paymentMethod = paymentMethod;
        this.cause         = cause;
        this.status        = DonationStatus.PENDING;
        this.donationDate  = LocalDateTime.now();
    }

    // ─── Getters & Setters ───────────────────────────────────────────────────────

    public Long getId()                        { return id; }
    public void setId(Long id)                 { this.id = id; }

    public String getDonorName()               { return donorName; }
    public void setDonorName(String n)         { this.donorName = n; }

    public String getDonorEmail()              { return donorEmail; }
    public void setDonorEmail(String e)        { this.donorEmail = e; }

    public BigDecimal getAmount()              { return amount; }
    public void setAmount(BigDecimal a)        { this.amount = a; }

    public String getPaymentMethod()           { return paymentMethod; }
    public void setPaymentMethod(String pm)    { this.paymentMethod = pm; }

    public String getCause()                   { return cause; }
    public void setCause(String c)             { this.cause = c; }

    public DonationStatus getStatus()          { return status; }
    public void setStatus(DonationStatus s)    { this.status = s; }

    public LocalDateTime getDonationDate()     { return donationDate; }
    public void setDonationDate(LocalDateTime d){ this.donationDate = d; }

    public String getTransactionId()           { return transactionId; }
    public void setTransactionId(String t)     { this.transactionId = t; }

    public String getRemarks()                 { return remarks; }
    public void setRemarks(String r)           { this.remarks = r; }

    @Override
    public String toString() {
        return "Donation{id=" + id + ", donor='" + donorName +
               "', amount=" + amount + ", status=" + status + "}";
    }
}
