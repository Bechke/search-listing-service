package rita.artha.shastra.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rita.artha.shastra.dto.*;
import rita.artha.shastra.entity.Conversation;
import rita.artha.shastra.entity.Message;
import rita.artha.shastra.entity.Person;
import rita.artha.shastra.repository.ConversationRepository;
import rita.artha.shastra.repository.MessageRepository;
import rita.artha.shastra.repository.PersonRepository;
import rita.artha.shastra.repository.VehicleRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepo;
    private final MessageRepository      messageRepo;
    private final PersonRepository       personRepo;
    private final VehicleRepository      vehicleRepo;

    // ── Start or retrieve an existing conversation + send first message ──────

    @Transactional
    public ConversationDto startOrGet(String buyerKeycloakId, CreateConversationRequest req) {
        Person buyer  = personRepo.findByKeycloakId(buyerKeycloakId)
                .orElseThrow(() -> new RuntimeException("Buyer not found: " + buyerKeycloakId));
        Person seller = personRepo.findByKeycloakId(req.getSellerKeycloakId())
                .orElseThrow(() -> new RuntimeException("Seller not found: " + req.getSellerKeycloakId()));

        Conversation conv = conversationRepo
                .findByVehicleSourceIdAndBuyer(req.getVehicleSourceId(), buyer)
                .orElseGet(() -> conversationRepo.save(Conversation.builder()
                        .vehicleSourceId(req.getVehicleSourceId())
                        .buyer(buyer)
                        .seller(seller)
                        .createdAt(LocalDateTime.now())
                        .lastMessageAt(LocalDateTime.now())
                        .build()));

        if (req.getFirstMessage() != null && !req.getFirstMessage().isBlank()) {
            Message msg = messageRepo.save(Message.builder()
                    .conversation(conv)
                    .sender(buyer)
                    .content(req.getFirstMessage().trim())
                    .sentAt(LocalDateTime.now())
                    .build());
            conv.setLastMessageAt(msg.getSentAt());
            conversationRepo.save(conv);
        }

        return toDto(conv, buyer);
    }

    // ── All conversations for the current user (buyer or seller) ────────────

    @Transactional(readOnly = true)
    public List<ConversationDto> getMyConversations(String keycloakId) {
        Person me = requirePerson(keycloakId);
        return conversationRepo.findByBuyerOrSellerOrderByLastMessageAtDesc(me, me)
                .stream().map(c -> toDto(c, me)).collect(Collectors.toList());
    }

    // ── All conversations for a specific ad (seller view) ───────────────────

    @Transactional(readOnly = true)
    public List<ConversationDto> getConversationsForAd(String vehicleSourceId, String keycloakId) {
        Person me = requirePerson(keycloakId);
        return conversationRepo.findByVehicleSourceIdOrderByLastMessageAtDesc(vehicleSourceId)
                .stream().map(c -> toDto(c, me)).collect(Collectors.toList());
    }

    // ── Get messages for a conversation (also marks incoming as read) ────────

    @Transactional
    public List<MessageDto> getMessages(Long conversationId, String keycloakId) {
        Person me   = requirePerson(keycloakId);
        Conversation conv = requireConversation(conversationId);
        messageRepo.markConversationRead(conv, me);
        return messageRepo.findByConversationOrderBySentAtAsc(conv)
                .stream().map(m -> toMessageDto(m, me)).collect(Collectors.toList());
    }

    // ── Send a reply in an existing conversation ─────────────────────────────

    @Transactional
    public MessageDto sendMessage(Long conversationId, String senderKeycloakId, SendMessageRequest req) {
        Person       sender = requirePerson(senderKeycloakId);
        Conversation conv   = requireConversation(conversationId);

        Message msg = messageRepo.save(Message.builder()
                .conversation(conv)
                .sender(sender)
                .content(req.getContent().trim())
                .sentAt(LocalDateTime.now())
                .build());

        conv.setLastMessageAt(msg.getSentAt());
        conversationRepo.save(conv);
        return toMessageDto(msg, sender);
    }

    // ── Total unread count across all conversations ──────────────────────────

    @Transactional(readOnly = true)
    public long getUnreadCount(String keycloakId) {
        return messageRepo.countUnreadForPerson(requirePerson(keycloakId));
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private Person requirePerson(String keycloakId) {
        return personRepo.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException("Person not found: " + keycloakId));
    }

    private Conversation requireConversation(Long id) {
        return conversationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Conversation not found: " + id));
    }

    private ConversationDto toDto(Conversation c, Person me) {
        MessageDto last = messageRepo.findFirstByConversationOrderBySentAtDesc(c)
                .map(m -> toMessageDto(m, me)).orElse(null);
        long unread = messageRepo.countUnreadInConversation(c, me);
        String vehicleTitle = vehicleRepo.findByVehicleSourceId(c.getVehicleSourceId())
                .map(v -> v.getTitle()).orElse(null);
        return ConversationDto.builder()
                .id(c.getId())
                .vehicleSourceId(c.getVehicleSourceId())
                .vehicleTitle(vehicleTitle)
                .buyer(personSummary(c.getBuyer()))
                .seller(personSummary(c.getSeller()))
                .lastMessageAt(c.getLastMessageAt())
                .lastMessage(last)
                .unreadCount(unread)
                .build();
    }

    private ConversationDto.PersonSummary personSummary(Person p) {
        return ConversationDto.PersonSummary.builder()
                .personId(p.getPersonId())
                .fullName(p.getFullName())
                .email(p.getEmail())
                .build();
    }

    private MessageDto toMessageDto(Message m, Person me) {
        return MessageDto.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .senderPersonId(m.getSender().getPersonId())
                .senderName(m.getSender().getFullName())
                .content(m.getContent())
                .sentAt(m.getSentAt())
                .mine(m.getSender().getPersonId().equals(me.getPersonId()))
                .build();
    }
}
