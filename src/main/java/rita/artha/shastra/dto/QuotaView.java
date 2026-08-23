package rita.artha.shastra.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Self-service view of a seller's plan + active listing quota. Powers the
 * mobile app's proactive "you're at your limit" check before letting the
 * user into the post-vehicle form — same numbers the backend enforces in
 * AdvertisementService/VehicleConsumerListener, just exposed read-only.
 */
@Data
@Builder
public class QuotaView {
    private String  planName;
    private int     listingLimit;
    private boolean boostEnabled;
    private long    activeListingCount;
    private boolean atLimit;
}
