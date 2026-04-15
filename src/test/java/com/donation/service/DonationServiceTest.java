package com.donation.service;

import com.donation.model.Donation;
import com.donation.repository.DonationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DonationService Unit Tests")
class DonationServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @InjectMocks
    private DonationService donationService;

    private Donation validDonation;

    @BeforeEach
    void setUp() {
        validDonation = new Donation(
                "John Doe",
                "john.doe@example.com",
                new BigDecimal("500.00"),
                "CREDIT_CARD",
                "EDUCATION"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PROCESS DONATION TESTS
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should process valid donation and set COMPLETED status")
    void testProcessDonation_Success() {
        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> inv.getArgument(0));

        Donation result = donationService.processDonation(validDonation);

        assertNotNull(result);
        assertEquals(Donation.DonationStatus.COMPLETED, result.getStatus());
        assertNotNull(result.getTransactionId());
        assertTrue(result.getTransactionId().startsWith("TXN-"));
        assertNotNull(result.getDonationDate());
        verify(donationRepository, times(1)).save(any(Donation.class));
    }

    @Test
    @DisplayName("Should set FAILED status when payment gateway declines (amount ends in .99)")
    void testProcessDonation_PaymentFailed() {
        validDonation.setAmount(new BigDecimal("100.99"));
        when(donationRepository.save(any(Donation.class))).thenAnswer(inv -> inv.getArgument(0));

        Donation result = donationService.processDonation(validDonation);

        assertEquals(Donation.DonationStatus.FAILED, result.getStatus());
        assertTrue(result.getRemarks().contains("declined"));
    }

    @Test
    @DisplayName("Should throw exception for null donation")
    void testProcessDonation_NullDonation() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> donationService.processDonation(null)
        );
        assertEquals("Donation cannot be null.", ex.getMessage());
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // VALIDATION TESTS
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should throw exception for blank donor name")
    void testValidation_BlankDonorName() {
        validDonation.setDonorName("");
        assertThrows(IllegalArgumentException.class,
                () -> donationService.validateDonation(validDonation));
    }

    @Test
    @DisplayName("Should throw exception for invalid email format")
    void testValidation_InvalidEmail() {
        validDonation.setDonorEmail("not-an-email");
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> donationService.validateDonation(validDonation)
        );
        assertTrue(ex.getMessage().contains("email"));
    }

    @Test
    @DisplayName("Should throw exception when amount is below minimum ($1.00)")
    void testValidation_AmountBelowMinimum() {
        validDonation.setAmount(new BigDecimal("0.50"));
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> donationService.validateDonation(validDonation)
        );
        assertTrue(ex.getMessage().contains("Minimum"));
    }

    @Test
    @DisplayName("Should throw exception when amount exceeds maximum ($100,000)")
    void testValidation_AmountAboveMaximum() {
        validDonation.setAmount(new BigDecimal("150000.00"));
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> donationService.validateDonation(validDonation)
        );
        assertTrue(ex.getMessage().contains("Maximum"));
    }

    @Test
    @DisplayName("Should throw exception for invalid payment method")
    void testValidation_InvalidPaymentMethod() {
        validDonation.setPaymentMethod("BITCOIN");
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> donationService.validateDonation(validDonation)
        );
        assertTrue(ex.getMessage().contains("Invalid payment method"));
    }

    @Test
    @DisplayName("Should accept all valid payment methods")
    void testValidation_AllValidPaymentMethods() {
        List<String> validMethods = List.of("CREDIT_CARD", "DEBIT_CARD", "UPI", "NET_BANKING");
        when(donationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        for (String method : validMethods) {
            validDonation.setPaymentMethod(method);
            assertDoesNotThrow(() -> donationService.processDonation(validDonation),
                    "Should accept payment method: " + method);
        }
    }

    @Test
    @DisplayName("Should throw exception for blank cause")
    void testValidation_BlankCause() {
        validDonation.setCause("");
        assertThrows(IllegalArgumentException.class,
                () -> donationService.validateDonation(validDonation));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // RETRIEVAL TESTS
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return donation by ID")
    void testGetDonationById_Found() {
        validDonation.setId(1L);
        when(donationRepository.findById(1L)).thenReturn(Optional.of(validDonation));

        Optional<Donation> result = donationService.getDonationById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    @DisplayName("Should return empty Optional for non-existent ID")
    void testGetDonationById_NotFound() {
        when(donationRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Donation> result = donationService.getDonationById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should return all donations for a valid email")
    void testGetDonationsByEmail_Success() {
        List<Donation> donations = Arrays.asList(validDonation, validDonation);
        when(donationRepository.findByDonorEmail("john.doe@example.com")).thenReturn(donations);

        List<Donation> result = donationService.getDonationsByEmail("john.doe@example.com");

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should throw exception for blank email in getDonationsByEmail")
    void testGetDonationsByEmail_BlankEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> donationService.getDonationsByEmail(""));
    }

    @Test
    @DisplayName("Should return total of completed donations")
    void testGetTotalDonations() {
        when(donationRepository.getTotalCompletedDonations()).thenReturn(new BigDecimal("1500.00"));

        BigDecimal total = donationService.getTotalDonations();

        assertEquals(new BigDecimal("1500.00"), total);
    }

    @Test
    @DisplayName("Should return ZERO when no completed donations")
    void testGetTotalDonations_NullReturnsZero() {
        when(donationRepository.getTotalCompletedDonations()).thenReturn(null);

        BigDecimal total = donationService.getTotalDonations();

        assertEquals(BigDecimal.ZERO, total);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // REFUND TESTS
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should refund a COMPLETED donation")
    void testRefundDonation_Success() {
        validDonation.setId(1L);
        validDonation.setStatus(Donation.DonationStatus.COMPLETED);
        when(donationRepository.findById(1L)).thenReturn(Optional.of(validDonation));
        when(donationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Donation refunded = donationService.refundDonation(1L);

        assertEquals(Donation.DonationStatus.REFUNDED, refunded.getStatus());
        assertTrue(refunded.getRemarks().contains("Refund processed"));
    }

    @Test
    @DisplayName("Should throw exception when refunding a non-COMPLETED donation")
    void testRefundDonation_NotCompleted() {
        validDonation.setId(1L);
        validDonation.setStatus(Donation.DonationStatus.PENDING);
        when(donationRepository.findById(1L)).thenReturn(Optional.of(validDonation));

        assertThrows(IllegalStateException.class,
                () -> donationService.refundDonation(1L));
    }

    @Test
    @DisplayName("Should throw exception when refunding non-existent donation")
    void testRefundDonation_NotFound() {
        when(donationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> donationService.refundDonation(999L));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // BOUNDARY / EDGE CASE TESTS
    // ─────────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should accept minimum valid amount ($1.00)")
    void testBoundary_MinimumAmount() {
        validDonation.setAmount(new BigDecimal("1.00"));
        when(donationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> donationService.processDonation(validDonation));
    }

    @Test
    @DisplayName("Should accept maximum valid amount ($100,000)")
    void testBoundary_MaximumAmount() {
        validDonation.setAmount(new BigDecimal("100000.00"));
        when(donationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> donationService.processDonation(validDonation));
    }

    @Test
    @DisplayName("Transaction ID should be unique per donation")
    void testTransactionId_Uniqueness() {
        when(donationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Donation d1 = donationService.processDonation(new Donation("Alice", "alice@test.com",
                new BigDecimal("100"), "UPI", "HEALTH"));
        Donation d2 = donationService.processDonation(new Donation("Bob", "bob@test.com",
                new BigDecimal("200"), "UPI", "HEALTH"));

        assertNotEquals(d1.getTransactionId(), d2.getTransactionId());
    }
}
