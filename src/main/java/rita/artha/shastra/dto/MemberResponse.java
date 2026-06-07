package rita.artha.shastra.dto;

import java.time.LocalDateTime;

public record MemberResponse(
    Integer personId, String keycloakId, String fullName, String email,
    String role, LocalDateTime joinedAt
) {}
