package rita.artha.shastra.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Tracks the active subscription plan for a seller (identified by Keycloak sub).
 *
 * Upserted by PaymentEventConsumer when PAYMENT_CAPTURED arrives
 * with purpose=SUBSCRIPTION_UPGRADE.
 *
 * Sellers who have never paid are treated as FREE (listing_limit=3, boost_enabled=false).
 */
@Entity
@Table(name = "user_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Keycloak subject — matches Person.keycloakId */
    @Column(name = "keycloak_id", nullable = false, unique = true)
    private String keycloakId;

    /** FREE | BASIC | PREMIUM */
    @Column(name = "plan_name", nullable = false)
    @Builder.Default
    private String planName = "FREE";

    @Column(name = "listing_limit", nullable = false)
    @Builder.Default
    private int listingLimit = 3;

    @Column(name = "boost_enabled", nullable = false)
    @Builder.Default
    private boolean boostEnabled = false;

    /** NULL means no expiry (FREE plan never expires). */
    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
