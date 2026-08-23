package rita.artha.shastra.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rita.artha.shastra.dto.AdminUserView;
import rita.artha.shastra.dto.PlanOverrideRequest;
import rita.artha.shastra.entity.UserPlan;
import rita.artha.shastra.service.AdminUserService;

import java.util.Arrays;
import java.util.Map;

/**
 * Admin-only user management: view sellers + their plan/listing counts, and
 * manually override a plan (comp/correction, bypasses payment). Suspend/reactivate
 * and role grant/revoke are Keycloak operations — see gateway-service's
 * AdminAccountController instead.
 *
 * Same access control pattern as AdminController: gateway enforces hasRole("ADMIN")
 * on the route, and this controller independently checks the forwarded X-User-Roles
 * header as a second layer of defence.
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User API", description = "Admin-only seller/plan management")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "Paginated list of sellers with plan + active listing count (admin only)")
    public ResponseEntity<?> getUsers(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        if (!isAdmin(request)) return forbidden();
        Page<AdminUserView> results = adminUserService.getUsers(PageRequest.of(page, size));
        return ResponseEntity.ok(results);
    }

    @PutMapping("/{keycloakId}/plan")
    @Operation(summary = "Manually override a seller's plan (admin only, bypasses payment)")
    public ResponseEntity<?> overridePlan(
            @PathVariable String keycloakId,
            @RequestBody PlanOverrideRequest req,
            HttpServletRequest request) {

        if (!isAdmin(request)) return forbidden();
        UserPlan updated = adminUserService.overridePlan(keycloakId, req);
        return ResponseEntity.ok(updated);
    }

    // ── helpers (mirrors AdminController) ───────────────────────────────────────

    private boolean isAdmin(HttpServletRequest request) {
        String rolesHeader = request.getHeader("X-User-Roles");
        if (rolesHeader == null || rolesHeader.isBlank()) return false;
        return Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .anyMatch("ADMIN"::equalsIgnoreCase);
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Access denied — ADMIN role required"));
    }
}
