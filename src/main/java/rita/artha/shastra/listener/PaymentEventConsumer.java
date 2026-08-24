package rita.artha.shastra.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rita.artha.shastra.dto.PaymentEvent;
import rita.artha.shastra.entity.Advertisement;
import rita.artha.shastra.entity.Organization;
import rita.artha.shastra.entity.UserPlan;
import rita.artha.shastra.repository.AdvertisementRepository;
import rita.artha.shastra.repository.OrganizationRepository;
import rita.artha.shastra.repository.UserPlanRepository;
import rita.artha.shastra.service.AdminService;
import rita.artha.shastra.service.PlanLimits;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Consumes "payment-events" and applies plan/boost changes to this service's data.
 *
 * Handles:
 *   PAYMENT_CAPTURED + purpose=SUBSCRIPTION_UPGRADE → upsert user_plans for the seller
 *   PAYMENT_CAPTURED + purpose=LISTING_BOOST        → mark the advertisement as boosted
 *
 * All other event types (PAYMENT_FAILED, REFUND_*, PAYMENT_SUCCESSFUL legacy) are either
 * forwarded to the legacy handler (PAYMENT_SUCCESSFUL) or silently ignored.
 *
 * Group ID is "search-listing-payment-group" — separate from notification-service's group
 * so both services receive every message independently.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final AdvertisementRepository advertisementRepository;
    private final UserPlanRepository      userPlanRepository;
    private final OrganizationRepository  organizationRepository;
    private final AdminService            adminService;

    private static final int BOOST_DAYS = 7;

    @Transactional
    @KafkaListener(
            topics = "payment-events",
            groupId = "search-listing-payment-group",
            containerFactory = "paymentEventListenerFactory")
    public void consume(PaymentEvent event) {
        if (event == null || event.getSellerId() == null) {
            log.warn("PaymentEventConsumer: received null or incomplete event, skipping");
            return;
        }

        log.info("PaymentEventConsumer: eventType={} purpose={} sellerId={}",
                event.getEventType(), event.getPurpose(), event.getSellerId());

        switch (nullSafe(event.getEventType())) {

            case "PAYMENT_CAPTURED" -> handleCaptured(event);

            // Legacy event type published before the new contract was in place
            case "PAYMENT_SUCCESSFUL" -> handleLegacyPaymentSuccessful(event);

            // Refund and failure events are handled by notification-service — ignore here
            default -> log.debug("PaymentEventConsumer: ignoring eventType={}", event.getEventType());
        }
    }

    // ─── PAYMENT_CAPTURED ────────────────────────────────────────────────────────

    private void handleCaptured(PaymentEvent event) {
        if ("SUBSCRIPTION_UPGRADE".equals(event.getPurpose())) {
            if (event.getOrganizationId() != null && !event.getOrganizationId().isBlank()) {
                activateOrgPlan(event.getOrganizationId(), event.getPlanId());
            } else {
                activatePersonalPlan(event.getSellerId(), event.getPlanId());
            }
            // Add-on: a boost bought alongside this plan purchase, one payment.
            // Runs after plan activation so the slot-capacity check below sees
            // the just-upgraded plan's (higher) featured_slots, not the old one.
            if (event.getListingId() != null && !event.getListingId().isBlank()) {
                activateBoost(event.getSellerId(), event.getListingId(), event.getBoostDurationDays());
            }
        } else if ("LISTING_BOOST".equals(event.getPurpose())) {
            activateBoost(event.getSellerId(), event.getListingId(), event.getBoostDurationDays());
        } else {
            log.warn("PaymentEventConsumer: PAYMENT_CAPTURED with unknown purpose={}", event.getPurpose());
        }
    }

    /**
     * Upserts the seller's user_plans row.
     * planId from the event is the plan name: FREE | BASIC | PREMIUM.
     */
    private void activatePersonalPlan(String keycloakId, String planId) {
        if (planId == null || planId.isBlank()) {
            log.error("PaymentEventConsumer: SUBSCRIPTION_UPGRADE missing planId for seller {}", keycloakId);
            return;
        }

        String planName    = planId.toUpperCase();
        int limit          = PlanLimits.personalLimitFor(planName);
        boolean boost      = PlanLimits.boostFor(planName);
        int featuredSlots  = PlanLimits.personalFeaturedSlotsFor(planName);

        UserPlan plan = userPlanRepository.findByKeycloakId(keycloakId)
                .orElse(UserPlan.builder().keycloakId(keycloakId).build());

        plan.setPlanName(planName);
        plan.setListingLimit(limit);
        plan.setBoostEnabled(boost);
        plan.setFeaturedSlots(featuredSlots);
        plan.setValidUntil(LocalDate.now().plusDays(30));
        userPlanRepository.save(plan);

        log.info("Activated personal plan {} for seller {} — limit={} boost={} featuredSlots={}",
                planName, keycloakId, limit, boost, featuredSlots);

        // A higher quota may unblock listings that were stuck waiting on payment.
        adminService.releaseFromPendingPayment(keycloakId);
    }

    /**
     * Same as activatePersonalPlan, but updates the organization's subscriptionTier
     * instead — a separate quota pool from any personal plan the buyer might also
     * have (see AdvertisementService.resolveIntakeStatus). Uses org-scaled limits
     * (PlanLimits.orgLimitFor) — same tier names as personal, higher numbers.
     */
    private void activateOrgPlan(String organizationId, String planId) {
        if (planId == null || planId.isBlank()) {
            log.error("PaymentEventConsumer: SUBSCRIPTION_UPGRADE missing planId for org {}", organizationId);
            return;
        }

        int orgId;
        try {
            orgId = Integer.parseInt(organizationId);
        } catch (NumberFormatException e) {
            log.error("PaymentEventConsumer: invalid organizationId={} in SUBSCRIPTION_UPGRADE event", organizationId);
            return;
        }

        Organization org = organizationRepository.findById(orgId).orElse(null);
        if (org == null) {
            log.error("PaymentEventConsumer: no Organization found for id={}", orgId);
            return;
        }

        String planName = planId.toUpperCase();
        org.setSubscriptionTier(planName);
        org.setUpdatedAt(LocalDateTime.now());
        organizationRepository.save(org);

        int limit = PlanLimits.orgLimitFor(planName);
        log.info("Activated org plan {} for organization {} — limit={}", planName, orgId, limit);

        // A higher quota may unblock this org's listings that were stuck waiting on payment.
        adminService.releaseOrgFromPendingPayment(orgId);
    }

    /**
     * Marks the advertisement as boosted for the purchased duration (3/7/30 days
     * — see payment-service's PaymentService.BOOST_PRICE_PAISE_BY_DAYS, the
     * price for each duration is fixed server-side there, not client-settable),
     * capped at the seller's (or their org's) plan featured_slots — a
     * concurrency cap, not a periodic allowance. If already at capacity, the
     * payment stays captured but the boost is not applied (see plan doc's
     * known-limitation note: there is no synchronous pre-payment check across
     * payment-service and this service, only the mobile app's quota pre-flight
     * via GET /ads/my/quota).
     */
    private void activateBoost(String keycloakId, String listingId, Integer boostDurationDays) {
        if (listingId == null || listingId.isBlank()) {
            log.error("PaymentEventConsumer: LISTING_BOOST missing listingId for seller {}", keycloakId);
            return;
        }
        int durationDays = boostDurationDays != null ? boostDurationDays : BOOST_DAYS;

        advertisementRepository.findByVehicleSourceId(listingId).ifPresentOrElse(ad -> {
            LocalDateTime now = LocalDateTime.now();
            int featuredSlots;
            long usedSlots;

            if (ad.getOrganization() != null) {
                Integer orgId = ad.getOrganization().getId();
                Organization org = organizationRepository.findById(orgId).orElse(null);
                featuredSlots = org != null ? PlanLimits.orgFeaturedSlotsFor(org.getSubscriptionTier()) : 0;
                usedSlots = advertisementRepository.countByOrganization_IdAndBoostedTrueAndBoostedUntilAfter(orgId, now);
            } else {
                UserPlan plan = userPlanRepository.findByKeycloakId(keycloakId).orElse(null);
                featuredSlots = plan != null ? plan.getFeaturedSlots() : 0;
                usedSlots = advertisementRepository.countByPerson_KeycloakIdAndBoostedTrueAndBoostedUntilAfter(keycloakId, now);
            }

            // Idempotent: a listing that's already boosted doesn't consume an extra slot to renew.
            if (!ad.isBoosted() && usedSlots >= featuredSlots) {
                log.warn("PaymentEventConsumer: seller {} has no free featured slots ({}/{} used) — "
                                + "boost for listing {} was paid but not applied",
                        keycloakId, usedSlots, featuredSlots, listingId);
                return;
            }

            LocalDateTime expiresAt = now.plusDays(durationDays);
            ad.setBoosted(true);
            ad.setBoostedUntil(expiresAt);
            ad.setUpdatedAt(now);
            advertisementRepository.save(ad);
            log.info("Listing {} boosted for {} days until {} for seller {}",
                    listingId, durationDays, expiresAt, keycloakId);
        }, () -> log.warn("PaymentEventConsumer: no Advertisement found for listingId={}", listingId));
    }

    // ─── Legacy handler ───────────────────────────────────────────────────────────

    /**
     * Handles the old PAYMENT_SUCCESSFUL event that moves a listing to PENDING_REVIEW.
     * Kept for backwards-compatibility during the transition period.
     */
    private void handleLegacyPaymentSuccessful(PaymentEvent event) {
        String listingId = event.getListingId();
        if (listingId == null) return;

        advertisementRepository.findByVehicleSourceId(listingId).ifPresentOrElse(ad -> {
            ad.setStatus("PENDING_REVIEW");
            ad.setUpdatedAt(LocalDateTime.now());
            advertisementRepository.save(ad);
            log.info("(legacy) Advertisement {} status → PENDING_REVIEW", listingId);
        }, () -> log.warn("(legacy) no Advertisement found for listingId={}", listingId));
    }

    // ─── Helper ───────────────────────────────────────────────────────────────────

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }
}
