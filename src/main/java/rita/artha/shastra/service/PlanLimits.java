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
    public static final int DEFAULT_ORG_FREE_LIMIT = 5;

    private static final Map<String, Integer> PERSONAL_LISTING_LIMITS = Map.of(
            "FREE",    3,
            "BASIC",   50,
            "PREMIUM", 200
    );

    private static final Map<String, Integer> ORG_LISTING_LIMITS = Map.of(
            "FREE",    5,
            "BASIC",   75,
            "PREMIUM", 300
    );

    private static final Map<String, Boolean> BOOST_ENABLED = Map.of(
            "FREE",    false,
            "BASIC",   true,
            "PREMIUM", true
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
}
