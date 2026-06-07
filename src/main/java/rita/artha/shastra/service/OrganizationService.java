package rita.artha.shastra.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rita.artha.shastra.dto.OrgCreateRequest;
import rita.artha.shastra.dto.OrgMemberRequest;
import rita.artha.shastra.dto.OrgResponse;
import rita.artha.shastra.dto.MemberResponse;
import rita.artha.shastra.entity.Organization;
import rita.artha.shastra.entity.OrganizationMember;
import rita.artha.shastra.entity.Person;
import rita.artha.shastra.repository.AdvertisementRepository;
import rita.artha.shastra.repository.OrganizationMemberRepository;
import rita.artha.shastra.repository.OrganizationRepository;
import rita.artha.shastra.repository.PersonRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository       orgRepo;
    private final OrganizationMemberRepository memberRepo;
    private final PersonRepository             personRepo;
    private final AdvertisementRepository      adRepo;

    @Transactional
    public OrgResponse createOrganization(OrgCreateRequest req, String keycloakId) {
        if (orgRepo.existsBySlug(req.slug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already taken");
        }
        Person owner = personRepo.findByKeycloakId(keycloakId)
                .orElseGet(() -> personRepo.save(Person.builder()
                        .keycloakId(keycloakId)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build()));

        Organization org = Organization.builder()
                .name(req.name())
                .slug(req.slug())
                .description(req.description())
                .owner(owner)
                .subscriptionTier("FREE")
                .status("ACTIVE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        org = orgRepo.save(org);

        OrganizationMember ownerMember = OrganizationMember.builder()
                .organization(org)
                .person(owner)
                .role("OWNER")
                .joinedAt(LocalDateTime.now())
                .build();
        memberRepo.save(ownerMember);

        return toResponse(org);
    }

    public OrgResponse getBySlug(String slug) {
        Organization org = orgRepo.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        return toResponse(org);
    }

    public OrgResponse getById(Integer id) {
        Organization org = orgRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        return toResponse(org);
    }

    public List<OrgResponse> searchOrganizations(String query) {
        if (query == null || query.trim().length() < 2) return List.of();
        return orgRepo.findByNameContainingIgnoreCase(query.trim())
                .stream().limit(8).map(this::toResponse).collect(Collectors.toList());
    }

    public List<OrgResponse> getMyOrganizations(String keycloakId) {
        return memberRepo.findByPerson_KeycloakId(keycloakId).stream()
                .map(m -> toResponse(m.getOrganization(), m.getRole()))
                .collect(Collectors.toList());
    }

    @Transactional
    public OrgResponse updateOrganization(Integer id, OrgCreateRequest req, String keycloakId) {
        Organization org = orgRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        requireOwner(org.getId(), keycloakId);

        if (!org.getSlug().equals(req.slug()) && orgRepo.existsBySlug(req.slug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Slug already taken");
        }
        org.setName(req.name());
        org.setSlug(req.slug());
        org.setDescription(req.description());
        org.setUpdatedAt(LocalDateTime.now());
        return toResponse(orgRepo.save(org));
    }

    public List<MemberResponse> getMembers(Integer orgId, String keycloakId) {
        requireMember(orgId, keycloakId);
        return memberRepo.findByOrganization_Id(orgId).stream()
                .map(this::toMemberResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MemberResponse addMember(Integer orgId, OrgMemberRequest req, String keycloakId) {
        requireOwner(orgId, keycloakId);
        Organization org = orgRepo.findById(orgId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Organization not found"));
        Person person = personRepo.findByKeycloakId(req.keycloakId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "User not found. They must have logged in at least once."));
        if (memberRepo.existsByOrganization_IdAndPerson_PersonId(orgId, person.getPersonId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member");
        }
        OrganizationMember member = OrganizationMember.builder()
                .organization(org)
                .person(person)
                .role(req.role() != null ? req.role() : "STAFF")
                .joinedAt(LocalDateTime.now())
                .build();
        return toMemberResponse(memberRepo.save(member));
    }

    @Transactional
    public MemberResponse updateMemberRole(Integer orgId, Integer personId, String role, String keycloakId) {
        requireOwner(orgId, keycloakId);
        OrganizationMember member = memberRepo
                .findByOrganization_IdAndPerson_PersonId(orgId, personId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        member.setRole(role);
        return toMemberResponse(memberRepo.save(member));
    }

    @Transactional
    public void removeMember(Integer orgId, Integer personId, String keycloakId) {
        requireOwner(orgId, keycloakId);
        OrganizationMember member = memberRepo
                .findByOrganization_IdAndPerson_PersonId(orgId, personId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        // Cannot remove the owner
        if ("OWNER".equals(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove the organization owner");
        }
        memberRepo.delete(member);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void requireOwner(Integer orgId, String keycloakId) {
        memberRepo.findByOrganization_IdAndPerson_KeycloakId(orgId, keycloakId)
                .filter(m -> "OWNER".equals(m.getRole()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Only the organization owner can perform this action"));
    }

    private void requireMember(Integer orgId, String keycloakId) {
        memberRepo.findByOrganization_IdAndPerson_KeycloakId(orgId, keycloakId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member"));
    }

    private OrgResponse toResponse(Organization org) {
        return toResponse(org, null);
    }

    private OrgResponse toResponse(Organization org, String myRole) {
        return new OrgResponse(
                org.getId(), org.getName(), org.getSlug(), org.getDescription(),
                org.getLogoPath(), org.getSubscriptionTier(), org.getStatus(),
                org.getCreatedAt(), org.getUpdatedAt(), myRole
        );
    }

    private MemberResponse toMemberResponse(OrganizationMember m) {
        Person p = m.getPerson();
        return new MemberResponse(
                p.getPersonId(), p.getKeycloakId(), p.getFullName(), p.getEmail(),
                m.getRole(), m.getJoinedAt()
        );
    }
}
