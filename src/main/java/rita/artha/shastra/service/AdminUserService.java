package rita.artha.shastra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rita.artha.shastra.dto.AdminUserView;
import rita.artha.shastra.dto.PlanOverrideRequest;
import rita.artha.shastra.entity.Person;
import rita.artha.shastra.entity.UserPlan;
import rita.artha.shastra.repository.AdvertisementRepository;
import rita.artha.shastra.repository.PersonRepository;
import rita.artha.shastra.repository.UserPlanRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * User-management side of admin-web: view sellers (Person + their plan + listing
 * count) and manually override a plan without going through payment (comps/
 * corrections). Account suspension and role grants are Keycloak operations and
 * live in gateway-service's AdminAccountController instead — this service has no
 * Keycloak Admin API access.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final PersonRepository        personRepository;
    private final UserPlanRepository      userPlanRepository;
    private final AdvertisementRepository advertisementRepository;

    private static final List<String> INACTIVE_STATUSES = List.of("REJECTED", "SOLD", "EXPIRED");

    public Page<AdminUserView> getUsers(Pageable pageable) {
        return personRepository.findAll(pageable).map(this::toView);
    }

    private AdminUserView toView(Person person) {
        UserPlan plan = userPlanRepository.findByKeycloakId(person.getKeycloakId()).orElse(null);
        long activeCount = advertisementRepository
                .countByPerson_KeycloakIdAndStatusNotIn(person.getKeycloakId(), INACTIVE_STATUSES);

        return AdminUserView.builder()
                .keycloakId(person.getKeycloakId())
                .fullName(person.getFullName())
                .email(person.getEmail())
                .mobileNumber(person.getMobileNumber())
                .company(person.getCompany())
                .planName(plan != null ? plan.getPlanName() : "FREE")
                .listingLimit(plan != null ? plan.getListingLimit() : PlanLimits.DEFAULT_FREE_LIMIT)
                .boostEnabled(plan != null && plan.isBoostEnabled())
                .validUntil(plan != null ? plan.getValidUntil() : null)
                .activeListingCount(activeCount)
                .build();
    }

    /** Manual admin override — bypasses payment entirely (comps/corrections). */
    @Transactional
    public UserPlan overridePlan(String keycloakId, PlanOverrideRequest req) {
        UserPlan plan = userPlanRepository.findByKeycloakId(keycloakId)
                .orElse(UserPlan.builder().keycloakId(keycloakId).build());

        if (req.getPlanName() != null) {
            plan.setPlanName(req.getPlanName().toUpperCase());
        }
        // Explicit listingLimit/boostEnabled win if provided; otherwise fall back to
        // the plan's standard tier limits so a bare planName change still makes sense.
        plan.setListingLimit(req.getListingLimit() != null
                ? req.getListingLimit() : PlanLimits.personalLimitFor(plan.getPlanName()));
        plan.setBoostEnabled(req.getBoostEnabled() != null
                ? req.getBoostEnabled() : PlanLimits.boostFor(plan.getPlanName()));
        plan.setValidUntil(req.getValidUntil());

        UserPlan saved = userPlanRepository.save(plan);
        log.info("Admin override: plan for seller {} -> {} (limit={}, boost={})",
                keycloakId, saved.getPlanName(), saved.getListingLimit(), saved.isBoostEnabled());
        return saved;
    }
}
