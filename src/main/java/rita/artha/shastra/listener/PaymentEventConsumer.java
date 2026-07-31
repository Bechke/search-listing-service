package rita.artha.shastra.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rita.artha.shastra.dto.PaymentEvent;
import rita.artha.shastra.entity.Advertisement;
import rita.artha.shastra.entity.UserPlan;
import rita.artha.shastra.repository.AdvertisementRepository;
import rita.artha.shastra.repository.UserPlanRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

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

    // Plan catalogue: planId (from payment-service) → listing limit
    // These match the seeded rows in payment-service's subscription_plans table.
    private static final Map<String, Integer> PLAN_LIMITS = Map.of(
            "FREE",    5,
            "BASIC",   50,
            "PREMIUM", 200
    );
    private static final Map<String, Boolean> PLAN_BOOST = Map.of(
            "FREE",    false,
            "BASIC",   true,
            "PREMIUM", true
    );
    private static final int BOOST_DAYS = 7;

    @Transactional
    @KafkaListener(topics = "payment-events", groupId = "search-listing-payment-group")
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
            activatePlan(event.getSellerId(), event.getPlanId());
        } else if ("LISTING_BOOST".equals(event.getPurpose())) {
            activateBoost(event.getSellerId(), event.getListingId());
        } else {
            log.warn("PaymentEventConsumer: PAYMENT_CAPTURED with unknown purpose={}", event.getPurpose());
        }
    }

    /**
     * Upserts the seller's user_plans row.
     * planId from the event is the plan name: FREE | BASIC | PREMIUM.
     */
    private void activatePlan(String keycloakId, String planId) {
        if (planId == null || planId.isBlank()) {
            log.error("PaymentEventConsumer: SUBSCRIPTION_UPGRADE missing planId for seller {}", keycloakId);
            return;
        }

        String planName = planId.toUpperCase();
        int limit       = PLAN_LIMITS.getOrDefault(planName, 5);
        boolean boost   = PLAN_BOOST.getOrDefault(planName, false);

        UserPlan plan = userPlanRepository.findByKeycloakId(keycloakId)
                .orElse(UserPlan.builder().keycloakId(keycloakId).build());

        plan.setPlanName(planName);
        plan.setListingLimit(limit);
        plan.setBoostEnabled(boost);
        plan.setValidUntil(LocalDate.now().plusDays(30));
        userPlanRepository.save(plan);

        log.info("Activated plan {} for seller {} — limit={} boost={}",
                planName, keycloakId, limit, boost);
    }

    /**
     * Marks the advertisement as boosted for BOOST_DAYS days.
     * The listing will surface at the top of search results until boostedUntil.
     */
    private void activateBoost(String keycloakId, String listingId) {
        if (listingId == null || listingId.isBlank()) {
            log.error("PaymentEventConsumer: LISTING_BOOST missing listingId for seller {}", keycloakId);
            return;
        }

        advertisementRepository.findByVehicleSourceId(listingId).ifPresentOrElse(ad -> {
            // Idempotent: if already boosted, extend the expiry from now
            LocalDateTime expiresAt = LocalDateTime.now().plusDays(BOOST_DAYS);
            ad.setBoosted(true);
            ad.setBoostedUntil(expiresAt);
            ad.setUpdatedAt(LocalDateTime.now());
            advertisementRepository.save(ad);
            log.info("Listing {} boosted until {} for seller {}", listingId, expiresAt, keycloakId);
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
