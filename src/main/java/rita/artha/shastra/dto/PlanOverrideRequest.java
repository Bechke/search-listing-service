package rita.artha.shastra.dto;

import lombok.Data;

import java.time.LocalDate;

/** Body for PUT /api/v1/admin/users/{keycloakId}/plan — admin manual override, bypasses payment. */
@Data
public class PlanOverrideRequest {
    /** FREE | BASIC | PREMIUM */
    private String planName;
    private Integer listingLimit;
    private Boolean boostEnabled;
    /** Optional — null means no expiry. */
    private LocalDate validUntil;
}
