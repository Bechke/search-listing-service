package rita.artha.shastra.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import rita.artha.shastra.dto.AdminListingDetail;
import rita.artha.shastra.dto.AdminListingSummary;
import rita.artha.shastra.dto.BulkStatusRequest;
import rita.artha.shastra.entity.Advertisement;
import rita.artha.shastra.entity.Person;
import rita.artha.shastra.entity.Vehicle;
import rita.artha.shastra.repository.AdvertisementRepository;
import rita.artha.shastra.repository.VehicleRepository;
import rita.artha.shastra.service.AdminService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Admin-only listing management endpoints.
 *
 * Access control: the API gateway validates the Keycloak JWT and forwards the
 * caller's realm roles as the {@code X-User-Roles} header.  Every endpoint
 * here checks for the {@code ADMIN} role before proceeding.  The gateway also
 * enforces {@code hasRole("ADMIN")} independently, giving two layers of defence.
 *
 * To grant admin access: assign the {@code ADMIN} realm role to the user in
 * Keycloak (Realm Roles → Create "ADMIN" → assign to user).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin API", description = "Admin-only listing management operations")
public class AdminController {

    private final AdminService            adminService;
    private final AdvertisementRepository advertisementRepository;
    private final VehicleRepository       vehicleRepository;
    private final ObjectMapper            objectMapper;

    /**
     * GET /api/v1/admin/listings/pending?page=0&size=20
     * Paginated list of all listings awaiting review, including who posted each one —
     * Advertisement.person is @JsonIgnore'd (LAZY, not meant for general API consumers),
     * so this maps into AdminListingSummary rather than returning the entity directly.
     */
    @GetMapping("/listings/pending")
    @Operation(summary = "List all pending-review listings (admin only)")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getPendingListings(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        if (!isAdmin(request)) return forbidden();
        Page<Advertisement> results = advertisementRepository
                .findByStatus("PENDING_REVIEW", PageRequest.of(page, size));
        return ResponseEntity.ok(results.map(this::toSummary));
    }

    /**
     * GET /api/v1/admin/listings/{vehicleSourceId}
     * Full vehicle detail (seller, full image gallery, description, price, brand/year/etc)
     * for admin-web's listing review panel — Advertisement alone doesn't carry these, and
     * the raw Vehicle entity only exposes images as an opaque JSON string with the seller
     * hidden behind @JsonIgnore, so this maps into AdminListingDetail instead.
     */
    @GetMapping("/listings/{vehicleSourceId}")
    @Operation(summary = "Get full vehicle detail for a listing (admin only)")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getListingDetail(
            @PathVariable String vehicleSourceId,
            HttpServletRequest request) {

        if (!isAdmin(request)) return forbidden();
        Optional<Vehicle> vehicle = vehicleRepository.findByVehicleSourceId(vehicleSourceId);
        return vehicle.<ResponseEntity<?>>map(v -> ResponseEntity.ok(toDetail(v)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * POST /api/v1/admin/listings/approve
     * Bulk-approve listings: sets status → ACTIVE, publishes vehicle-ads events
     * so notification-service notifies each seller.
     *
     * Request body: { "listingIds": ["id1", "id2", ...] }
     */
    @PostMapping("/listings/approve")
    @Operation(summary = "Bulk approve listings (admin only)")
    public ResponseEntity<?> bulkApprove(
            @RequestBody BulkStatusRequest req,
            HttpServletRequest request) {

        if (!isAdmin(request)) return forbidden();
        if (req.getListingIds() == null || req.getListingIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "listingIds must not be empty"));
        }

        int updated = adminService.bulkApprove(req.getListingIds());
        log.info("Admin bulk approve: {}/{} listings activated by {}",
                updated, req.getListingIds().size(), adminId(request));
        return ResponseEntity.ok(Map.of(
                "approved",  updated,
                "requested", req.getListingIds().size()));
    }

    /**
     * POST /api/v1/admin/listings/reject
     * Bulk-reject listings: sets status → REJECTED, publishes vehicle-ads events
     * so notification-service notifies each seller.
     *
     * Request body: { "listingIds": ["id1", "id2", ...], "reason": "optional text" }
     */
    @PostMapping("/listings/reject")
    @Operation(summary = "Bulk reject listings (admin only)")
    public ResponseEntity<?> bulkReject(
            @RequestBody BulkStatusRequest req,
            HttpServletRequest request) {

        if (!isAdmin(request)) return forbidden();
        if (req.getListingIds() == null || req.getListingIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "listingIds must not be empty"));
        }

        int updated = adminService.bulkReject(req.getListingIds(), req.getReason());
        log.info("Admin bulk reject: {}/{} listings rejected by {} reason='{}'",
                updated, req.getListingIds().size(), adminId(request), req.getReason());
        return ResponseEntity.ok(Map.of(
                "rejected",  updated,
                "requested", req.getListingIds().size()));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AdminListingSummary toSummary(Advertisement ad) {
        Person seller = ad.getPerson();
        return AdminListingSummary.builder()
                .advertisementId(ad.getAdvertisementId())
                .vehicleSourceId(ad.getVehicleSourceId())
                .title(ad.getTitle())
                .status(ad.getStatus())
                .rejectionReason(ad.getRejectionReason())
                .adCategory(ad.getAdCategory())
                .adSubcategory(ad.getAdSubcategory())
                .defaultImgPath(ad.getDefaultImgPath())
                .country(ad.getCountry())
                .state(ad.getState())
                .city(ad.getCity())
                .boosted(ad.isBoosted())
                .boostedUntil(ad.getBoostedUntil())
                .createdAt(ad.getCreatedAt())
                .updatedAt(ad.getUpdatedAt())
                .sellerKeycloakId(seller != null ? seller.getKeycloakId() : null)
                .sellerName(seller != null ? seller.getFullName() : null)
                .sellerEmail(seller != null ? seller.getEmail() : null)
                .sellerMobile(seller != null ? seller.getMobileNumber() : null)
                .build();
    }

    private AdminListingDetail toDetail(Vehicle vehicle) {
        Person seller = vehicle.getPerson();
        return AdminListingDetail.builder()
                .vehicleId(vehicle.getVehicleId())
                .vehicleSourceId(vehicle.getVehicleSourceId())
                .adSubcategory(vehicle.getAdSubcategory())
                .brand(vehicle.getBrand())
                .year(vehicle.getYear())
                .fuelType(vehicle.getFuelType())
                .transmission(vehicle.getTransmission())
                .odometerReading(vehicle.getOdometerReading())
                .numOwners(vehicle.getNumOwners())
                .title(vehicle.getTitle())
                .description(vehicle.getDescription())
                .price(vehicle.getPrice())
                .defaultImgPath(vehicle.getDefaultImgPath())
                .imageUrls(parseImageUrls(vehicle.getImageUrlsJson()))
                .country(vehicle.getCountry())
                .state(vehicle.getState())
                .city(vehicle.getCity())
                .neighbourhood(vehicle.getNeighbourhood())
                .status(vehicle.getStatus())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .sellerKeycloakId(seller != null ? seller.getKeycloakId() : null)
                .sellerName(seller != null ? seller.getFullName() : null)
                .sellerEmail(seller != null ? seller.getEmail() : null)
                .sellerMobile(seller != null ? seller.getMobileNumber() : null)
                .build();
    }

    private List<String> parseImageUrls(String imageUrlsJson) {
        if (imageUrlsJson == null || imageUrlsJson.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(imageUrlsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Could not parse imageUrlsJson: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Returns true when the gateway-forwarded X-User-Roles header contains ADMIN.
     * The header is set by {@code UserIdHeaderFilter} in gateway-service after
     * the JWT is validated and {@code realm_access.roles} is extracted.
     */
    private boolean isAdmin(HttpServletRequest request) {
        String rolesHeader = request.getHeader("X-User-Roles");
        if (rolesHeader == null || rolesHeader.isBlank()) return false;
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch("ADMIN"::equalsIgnoreCase);
    }

    private String adminId(HttpServletRequest request) {
        String id = request.getHeader("X-User-Id");
        return id != null ? id : "unknown";
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Access denied — ADMIN role required"));
    }
}
