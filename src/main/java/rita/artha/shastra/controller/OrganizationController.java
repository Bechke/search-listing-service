package rita.artha.shastra.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rita.artha.shastra.dto.*;
import rita.artha.shastra.service.OrganizationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orgs")
@RequiredArgsConstructor
@Tag(name = "Organization API", description = "Endpoints for managing organizations")
public class OrganizationController {

    private final OrganizationService orgService;

    @GetMapping("/search")
    @Operation(summary = "Search organizations by name (public)")
    public List<OrgResponse> search(@RequestParam(defaultValue = "") String q) {
        return orgService.searchOrganizations(q);
    }

    @PostMapping
    @Operation(summary = "Create a new organization")
    public OrgResponse create(
            @RequestBody OrgCreateRequest req,
            @RequestHeader(value = "X-User-Id", required = false) String keycloakId) {
        return orgService.createOrganization(req, keycloakId);
    }

    @GetMapping("/my")
    @Operation(summary = "Get organizations the current user belongs to")
    public List<OrgResponse> getMyOrgs(
            @RequestHeader(value = "X-User-Id", required = false) String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) return List.of();
        return orgService.getMyOrganizations(keycloakId);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get organization by slug (public)")
    public OrgResponse getBySlug(@PathVariable String slug) {
        return orgService.getBySlug(slug);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update organization profile")
    public OrgResponse update(
            @PathVariable Integer id,
            @RequestBody OrgCreateRequest req,
            @RequestHeader(value = "X-User-Id", required = false) String keycloakId) {
        return orgService.updateOrganization(id, req, keycloakId);
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List organization members")
    public List<MemberResponse> getMembers(
            @PathVariable Integer id,
            @RequestHeader(value = "X-User-Id", required = false) String keycloakId) {
        return orgService.getMembers(id, keycloakId);
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add a member (Owner only)")
    public MemberResponse addMember(
            @PathVariable Integer id,
            @RequestBody OrgMemberRequest req,
            @RequestHeader(value = "X-User-Id", required = false) String keycloakId) {
        return orgService.addMember(id, req, keycloakId);
    }

    @PutMapping("/{id}/members/{personId}")
    @Operation(summary = "Update member role (Owner only)")
    public MemberResponse updateMemberRole(
            @PathVariable Integer id,
            @PathVariable Integer personId,
            @RequestParam String role,
            @RequestHeader(value = "X-User-Id", required = false) String keycloakId) {
        return orgService.updateMemberRole(id, personId, role, keycloakId);
    }

    @DeleteMapping("/{id}/members/{personId}")
    @Operation(summary = "Remove a member (Owner only)")
    public void removeMember(
            @PathVariable Integer id,
            @PathVariable Integer personId,
            @RequestHeader(value = "X-User-Id", required = false) String keycloakId) {
        orgService.removeMember(id, personId, keycloakId);
    }
}
