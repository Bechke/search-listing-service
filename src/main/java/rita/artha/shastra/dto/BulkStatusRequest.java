package rita.artha.shastra.dto;

import lombok.Data;
import java.util.List;

/**
 * Request body for admin bulk approve / reject operations.
 */
@Data
public class BulkStatusRequest {
    /** vehicleSourceId values (from vehicle-service) to act on */
    private List<String> listingIds;
    /** Optional rejection reason, included in the Kafka event data field */
    private String reason;
}
