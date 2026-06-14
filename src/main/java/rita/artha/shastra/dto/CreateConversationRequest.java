package rita.artha.shastra.dto;

import lombok.Data;

@Data
public class CreateConversationRequest {
    private String vehicleSourceId;
    private String sellerKeycloakId;
    private String firstMessage;
}
