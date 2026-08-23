package rita.artha.shastra.service;

import rita.artha.shastra.dto.QuotaView;
import rita.artha.shastra.entity.Advertisement;
import rita.artha.shastra.entity.UserPlan;
import rita.artha.shastra.repository.AdvertisementRepository;
import rita.artha.shastra.repository.UserPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdvertisementService {

    private final AdvertisementRepository advertisementRepository;
    private final UserPlanRepository      userPlanRepository;

    // Statuses that no longer occupy a listing slot
    private static final List<String> INACTIVE_STATUSES = List.of("REJECTED", "SOLD", "EXPIRED");

    // Default limit for sellers who have no user_plans row (treated as FREE)
    private static final int DEFAULT_FREE_LIMIT = PlanLimits.DEFAULT_FREE_LIMIT;

    // ─── Queries ─────────────────────────────────────────────────────────────────

    public List<Advertisement> getAllAdvertisements() {
        return advertisementRepository.findAll();
    }

    public Optional<Advertisement> getAdvertisementById(Integer id) {
        return advertisementRepository.findById(id);
    }

    public void deleteAdvertisement(Integer id) {
        advertisementRepository.deleteById(id);
    }

    public Page<Advertisement> search(
            String country, String state, String city,
            String neighbourhood, Pageable pageable) {

        if (StringUtils.hasText(neighbourhood)) {
            return advertisementRepository
                    .findByCountryAndStateAndCityAndNeighbourhood(
                            country, state, city, neighbourhood, pageable);
        }
        return advertisementRepository.findByCountryAndStateAndCity(country, state, city, pageable);
    }

    // ─── Save with limit enforcement ─────────────────────────────────────────────

    /**
     * Persists a new or updated advertisement.
     *
     * For NEW listings (advertisementId == null) the seller's active listing count
     * is checked against their plan limit. If the limit is reached an HTTP 429 is
     * thrown so the caller receives a clear "upgrade your plan" message.
     *
     * Updates to existing listings bypass the limit check.
     */
    public Advertisement saveAdvertisement(Advertisement ad) {
        boolean isNew = (ad.getAdvertisementId() == null);

        if (isNew && ad.getPerson() != null) {
            String keycloakId = ad.getPerson().getKeycloakId();
            enforceListingLimit(keycloakId);
        }

        return advertisementRepository.save(ad);
    }

    // ─── Limit enforcement helpers ────────────────────────────────────────────────

    /**
     * Throws HTTP 429 (Too Many Requests) if the seller has reached their plan limit.
     *
     * Active listing count = all listings NOT in [REJECTED, SOLD, EXPIRED].
     * Plan limit is read from user_plans; sellers without a row default to FREE (5).
     */
    public void enforceListingLimit(String keycloakId) {
        int limit = getPlanLimit(keycloakId);

        long activeCount = advertisementRepository
                .countByPerson_KeycloakIdAndStatusNotIn(keycloakId, INACTIVE_STATUSES);

        if (activeCount >= limit) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Listing limit reached (" + activeCount + "/" + limit + "). "
                            + "Upgrade your plan to post more listings."
            );
        }
    }

    /** Returns the listing limit for a seller, defaulting to FREE if no plan exists. */
    public int getPlanLimit(String keycloakId) {
        return userPlanRepository.findByKeycloakId(keycloakId)
                .map(UserPlan::getListingLimit)
                .orElse(DEFAULT_FREE_LIMIT);
    }

    /** Returns the seller's current plan, or a synthetic FREE plan if none exists. */
    public UserPlan getPlanOrDefault(String keycloakId) {
        return userPlanRepository.findByKeycloakId(keycloakId)
                .orElse(UserPlan.builder()
                        .keycloakId(keycloakId)
                        .planName("FREE")
                        .listingLimit(DEFAULT_FREE_LIMIT)
                        .boostEnabled(false)
                        .build());
    }

    /**
     * Self-service quota snapshot for the mobile app — lets it check "am I at my
     * limit" and route straight to the upgrade screen before the user fills out
     * a whole post form, instead of only finding out after Kafka round-trips the
     * PENDING_PAYMENT status back. Personal listings only — org quota is a
     * separate pool (see resolveIntakeStatus) and isn't surfaced here yet.
     */
    public QuotaView getQuota(String keycloakId) {
        UserPlan plan = getPlanOrDefault(keycloakId);
        long activeCount = advertisementRepository
                .countByPerson_KeycloakIdAndStatusNotIn(keycloakId, INACTIVE_STATUSES);

        return QuotaView.builder()
                .planName(plan.getPlanName())
                .listingLimit(plan.getListingLimit())
                .boostEnabled(plan.isBoostEnabled())
                .activeListingCount(activeCount)
                .atLimit(activeCount >= plan.getListingLimit())
                .build();
    }

    // ─── Intake status decision (PENDING_REVIEW vs PENDING_PAYMENT) ──────────────

    /**
     * Called by VehicleConsumerListener whenever a listing arrives with
     * status=PENDING_REVIEW from vehicle-service (new post or seller resubmit).
     * Decides whether the seller (or their org) is within their plan's active-
     * listing quota. Personal listings check the seller's own UserPlan;
     * org listings check the organization's subscriptionTier against the same
     * FREE/BASIC/PREMIUM limit table — two independent quota pools.
     *
     * @param keycloakId          seller's Keycloak sub (always required — org listings
     *                            are still posted by a person)
     * @param organizationId      null for personal listings
     * @param orgSubscriptionTier the org's subscriptionTier; ignored when organizationId is null
     * @return "PENDING_REVIEW" if under quota, "PENDING_PAYMENT" if at/over quota
     */
    public String resolveIntakeStatus(String keycloakId, Integer organizationId, String orgSubscriptionTier) {
        int limit;
        long activeCount;

        if (organizationId != null) {
            limit = PlanLimits.orgLimitFor(orgSubscriptionTier);
            activeCount = advertisementRepository
                    .countByOrganization_IdAndStatusNotIn(organizationId, INACTIVE_STATUSES);
        } else {
            limit = getPlanLimit(keycloakId);
            activeCount = advertisementRepository
                    .countByPerson_KeycloakIdAndStatusNotIn(keycloakId, INACTIVE_STATUSES);
        }

        return activeCount >= limit ? "PENDING_PAYMENT" : "PENDING_REVIEW";
    }
}
