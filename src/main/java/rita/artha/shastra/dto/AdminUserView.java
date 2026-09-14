package rita.artha.shastra.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Row shape for GET /api/v1/admin/users — admin-web's user management table. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserView {
    private String keycloakId;
    private String fullName;
    private String email;
    private String mobileNumber;
    private String company;

    private String planName;
    private int    listingLimit;
    private boolean boostEnabled;
    private LocalDate validUntil;

    private long activeListingCount;
}
