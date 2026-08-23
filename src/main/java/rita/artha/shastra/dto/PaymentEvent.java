package rita.artha.shastra.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Consumed from the "payment-events" Kafka topic.
 * Published by payment-service for all payment-related lifecycle events.
 *
 * Event types relevant to search-listing-service:
 *   PAYMENT_CAPTURED  — payment succeeded
 *     purpose=SUBSCRIPTION_UPGRADE → update user_plans for sellerId
 *     purpose=LISTING_BOOST        → mark the listing as boosted for 7 days
 *
 * Other event types (PAYMENT_FAILED, REFUND_*, etc.) are ignored here —
 * they are handled by notification-service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private String eventType;          // PAYMENT_CAPTURED | PAYMENT_FAILED | REFUND_* ...

    private String paymentId;          // Razorpay payment ID
    private String orderId;            // Razorpay order ID

    private String sellerId;           // Keycloak sub
    private String sellerEmail;
    private String sellerName;

    /**
     * Set only when purpose=SUBSCRIPTION_UPGRADE targets an organization's plan
     * rather than sellerId's own personal plan. Null means personal.
     */
    private String organizationId;

    /** SUBSCRIPTION_UPGRADE or LISTING_BOOST */
    private String purpose;

    private String planId;             // set when purpose = SUBSCRIPTION_UPGRADE
    private String listingId;          // set when purpose = LISTING_BOOST (also legacy field)

    private Long   amount;             // in paise
    private String currency;

    private String rejectionReason;    // set on REFUND_REJECTED
    private String transactionRef;
}
