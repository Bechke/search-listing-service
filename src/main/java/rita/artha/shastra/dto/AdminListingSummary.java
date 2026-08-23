package rita.artha.shastra.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Row shape for admin-web's pending-listings table — an Advertisement plus the
 * seller who posted it. Advertisement.person is @JsonIgnore'd (LAZY, and not
 * meant for general API consumers), so the raw entity can't surface this;
 * AdminController maps into this DTO instead of returning Advertisement directly.
 */
@Data
@Builder
public class AdminListingSummary {
    private Integer advertisementId;
    private String vehicleSourceId;
    private String title;
    private String status;
    private String rejectionReason;
    private String adCategory;
    private String adSubcategory;
    private String defaultImgPath;
    private String country;
    private String state;
    private String city;
    private boolean boosted;
    private LocalDateTime boostedUntil;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Seller (Advertisement.person) ──────────────────────────────────────────
    private String sellerKeycloakId;
    private String sellerName;
    private String sellerEmail;
    private String sellerMobile;
}
