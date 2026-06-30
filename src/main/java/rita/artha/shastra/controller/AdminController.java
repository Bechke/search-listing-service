package rita.artha.shastra.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rita.artha.shastra.dto.BulkStatusRequest;
import rita.artha.shastra.entity.Advertisement;
import rita.artha.shastra.repository.AdvertisementRepository;
import rita.artha.shastra.service.AdminService;

import java.util.Arrays;
import java.util.Map;

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

    /**
     * GET /api/v1/admin/listings/pending?page=0&size=20
     * Paginated list of all listings awaiting review.
     */
    @GetMapping("/listings/pending")
    @Operation(summary = "List all pending-review listings (admin only)")
    public ResponseEntity<?> getPendingListings(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        if (!isAdmin(request)) return forbidden();
        Page<Advertisement> results = advertisementRepository
                .findByStatus("PENDING_REVIEW", PageRequest.of(page, size));
        return ResponseEntity.ok(results);
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
