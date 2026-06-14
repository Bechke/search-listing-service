package rita.artha.shastra.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class MessageDto {
    private Long          id;
    private Long          conversationId;
    private Integer       senderPersonId;
    private String        senderName;
    private String        content;
    private LocalDateTime sentAt;
    private boolean       mine;
}
