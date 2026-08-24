package rita.artha.shastra.service;

import java.util.Map;

/**
 * Single source of truth for FREE/BASIC/PREMIUM listing quotas and boost
 * eligibility. Mirrors the seeded rows in payment-service's subscription_plans
 * table — same tier names for personal and org context, but two separate
 * price/limit lists (payment-service's subscription_plans.context column).
 * Org limits are intentionally higher — a dealer org's "BASIC" isn't the same
 * scale as an individual seller's "BASIC".
 */
public final class PlanLimits {

    private PlanLimits() {}

    public static final int DEFAULT_FREE_LIMIT = 3;
    public static final int DEFAULT_ORG_FREE_LIMIT = 3;

    private static final Map<String, Integer> PERSONAL_LISTING_LIMITS = Map.of(
            "FREE",    3,
            "BASIC",   10,
            "PREMIUM", 30
    );

    private static final Map<String, Integer> ORG_LISTING_LIMITS = Map.of(
            "FREE",    3,
            "BASIC",   25,
            "PREMIUM", 100
    );

    private static final Map<String, Boolean> BOOST_ENABLED = Map.of(
            "FREE",    false,
            "BASIC",   true,
            "PREMIUM", true
    );

    /**
     * Max listings a seller may have boosted (featured) at once — a concurrency
     * cap, not a periodic allowance. A boost occupies a slot for its 7-day
     * boostedUntil window (PaymentEventConsumer.activateBoost) and the slot
     * self-frees once that passes; nothing resets this on a schedule.
     */
    private static final Map<String, Integer> PERSONAL_FEATURED_SLOTS = Map.of(
            "FREE",    0,
            "BASIC",   2,
            "PREMIUM", 10
    );

    private static final Map<String, Integer> ORG_FEATURED_SLOTS = Map.of(
            "FREE",    0,
            "BASIC",   4,
            "PREMIUM", 15
    );

    public static int personalLimitFor(String planName) {
        String key = planName == null ? "FREE" : planName.toUpperCase();
        return PERSONAL_LISTING_LIMITS.getOrDefault(key, DEFAULT_FREE_LIMIT);
    }

    public static int orgLimitFor(String planName) {
        String key = planName == null ? "FREE" : planName.toUpperCase();
        return ORG_LISTING_LIMITS.getOrDefault(key, DEFAULT_ORG_FREE_LIMIT);
    }

    public static boolean boostFor(String planName) {
        String key = planName == null ? "FREE" : planName.toUpperCase();
        return BOOST_ENABLED.getOrDefault(key, false);
    }

    /** Mirrors payment-service's subscription_plans.featured_slots (context=PERSONAL). */
    public static int personalFeaturedSlotsFor(String planName) {
        String key = planName == null ? "FREE" : planName.toUpperCase();
        return PERSONAL_FEATURED_SLOTS.getOrDefault(key, 0);
    }

    /** Mirrors payment-service's subscription_plans.featured_slots (context=ORG). */
    public static int orgFeaturedSlotsFor(String planName) {
        String key = planName == null ? "FREE" : planName.toUpperCase();
        return ORG_FEATURED_SLOTS.getOrDefault(key, 0);
    }
}
