package rita.artha.shastra.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ConversationDto {

    private Long   id;
    private String vehicleSourceId;
    private String vehicleTitle;
    private PersonSummary buyer;
    private PersonSummary seller;
    private LocalDateTime lastMessageAt;
    private MessageDto    lastMessage;
    private long          unreadCount;

    @Data
    @Builder
    public static class PersonSummary {
        private Integer personId;
        private String  fullName;
        private String  email;
    }
}
