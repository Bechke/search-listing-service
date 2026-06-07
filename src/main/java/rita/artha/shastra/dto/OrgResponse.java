package rita.artha.shastra.dto;

import java.time.LocalDateTime;

public record OrgResponse(
    Integer id, String name, String slug, String description,
    String logoPath, String subscriptionTier, String status,
    LocalDateTime createdAt, LocalDateTime updatedAt,
    String myRole
) {}
