package rita.artha.shastra.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDTO {

    /** CREATE | UPDATE | DELETE */
    private String eventType;

    /** MongoDB _id from vehicle-service */
    private String vehicleId;

    /** Keycloak sub claim of the seller */
    private String sellerId;

    /** Keycloak email / preferred_username */
    private String sellerEmail;

    // ── Listing ────────────────────────────────────────────────────────────────
    private String title;
    private String description;
    private Double price;
    private String currency;

    // ── Category ───────────────────────────────────────────────────────────────
    private String adSubcategory;   // CAR | BIKE | SCOOTER | etc.

    // ── Vehicle filters ────────────────────────────────────────────────────────
    private String brand;
    private Integer year;
    private String fuelType;
    private String transmission;
    private Integer odometerReading;
    private Integer numOwners;

    // ── Location ───────────────────────────────────────────────────────────────
    private String country;
    private String state;
    private String city;
    private String neighbourhood;

    // ── Images ─────────────────────────────────────────────────────────────────
    private String defaultImgPath;
    private List<String> imageUrls;

    // ── Status & timestamps ────────────────────────────────────────────────────
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Organization ───────────────────────────────────────────────────────────
    /** Organization DB id as string; null for personal listings */
    private String organizationId;

    // ── Legacy field kept for manual Kafka sends via VehicleKafkaController ────
    /** @deprecated use sellerId (Keycloak sub) instead */
    @Deprecated
    private Integer personId;
    private String mobile;
}
