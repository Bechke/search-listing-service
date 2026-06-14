package rita.artha.shastra.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rita.artha.shastra.dto.*;
import rita.artha.shastra.service.ConversationService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    /** Start a conversation (or return existing) + send first message */
    @PostMapping
    public ResponseEntity<ConversationDto> startConversation(
            @RequestHeader("X-User-Id") String keycloakId,
            @RequestBody CreateConversationRequest req) {
        return ResponseEntity.ok(conversationService.startOrGet(keycloakId, req));
    }

    /** All conversations the current user is part of (buyer or seller) */
    @GetMapping("/my")
    public ResponseEntity<List<ConversationDto>> getMyConversations(
            @RequestHeader("X-User-Id") String keycloakId) {
        return ResponseEntity.ok(conversationService.getMyConversations(keycloakId));
    }

    /** All conversations for a specific ad — used by seller to see all buyer threads */
    @GetMapping("/ad/{vehicleSourceId}")
    public ResponseEntity<List<ConversationDto>> getConversationsForAd(
            @PathVariable String vehicleSourceId,
            @RequestHeader("X-User-Id") String keycloakId) {
        return ResponseEntity.ok(conversationService.getConversationsForAd(vehicleSourceId, keycloakId));
    }

    /** Get all messages in a thread; marks incoming messages as read */
    @GetMapping("/{id}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String keycloakId) {
        return ResponseEntity.ok(conversationService.getMessages(id, keycloakId));
    }

    /** Send a reply in an existing conversation */
    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageDto> sendMessage(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String keycloakId,
            @RequestBody SendMessageRequest req) {
        return ResponseEntity.ok(conversationService.sendMessage(id, keycloakId, req));
    }

    /** Total unread message count across all conversations */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader("X-User-Id") String keycloakId) {
        return ResponseEntity.ok(Map.of("count", conversationService.getUnreadCount(keycloakId)));
    }
}
