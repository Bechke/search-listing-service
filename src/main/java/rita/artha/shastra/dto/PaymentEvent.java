package rita.artha.shastra.dto;

import lombok.Data;

/**
 * Consumed from the "payment-events" Kafka topic.
 * Published by the payment service when a payment is processed.
 */
@Data
public class PaymentEvent {
    /** Event discriminator — currently only "PAYMENT_SUCCESSFUL" */
    private String eventType;
    /** Keycloak sub of the seller who paid */
    private String sellerId;
    /** The vehicleSourceId of the listing that was paid for */
    private String listingId;
    /** Payment service internal ID */
    private String paymentId;
}
