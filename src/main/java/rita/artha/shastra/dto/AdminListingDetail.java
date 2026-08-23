package rita.artha.shastra.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full listing detail for admin-web's review panel — a Vehicle row plus the
 * seller who posted it and the full image gallery (Vehicle.imageUrlsJson,
 * parsed — the raw entity only exposes it as an opaque JSON string, and
 * Vehicle.person is @JsonIgnore'd). AdminController builds this instead of
 * returning the Vehicle entity directly.
 */
@Data
@Builder
public class AdminListingDetail {
    private Integer vehicleId;
    private String vehicleSourceId;
    private String adSubcategory;
    private String brand;
    private Integer year;
    private String fuelType;
    private String transmission;
    private Integer odometerReading;
    private Integer numOwners;
    private String title;
    private String description;
    private Double price;
    private String defaultImgPath;
    /** All listing images, parsed from Vehicle.imageUrlsJson — empty list if none. */
    private List<String> imageUrls;
    private String country;
    private String state;
    private String city;
    private String neighbourhood;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Seller (Vehicle.person) ─────────────────────────────────────────────────
    private String sellerKeycloakId;
    private String sellerName;
    private String sellerEmail;
    private String sellerMobile;
}
