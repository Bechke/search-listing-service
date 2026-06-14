package rita.artha.shastra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import rita.artha.shastra.entity.Conversation;
import rita.artha.shastra.entity.Message;
import rita.artha.shastra.entity.Person;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationOrderBySentAtAsc(Conversation conversation);

    Optional<Message> findFirstByConversationOrderBySentAtDesc(Conversation conversation);

    @Query("SELECT COUNT(m) FROM Message m " +
           "JOIN m.conversation c " +
           "WHERE (c.buyer = :person OR c.seller = :person) " +
           "AND m.sender <> :person AND m.readAt IS NULL")
    long countUnreadForPerson(@Param("person") Person person);

    @Query("SELECT COUNT(m) FROM Message m " +
           "WHERE m.conversation = :conv AND m.sender <> :person AND m.readAt IS NULL")
    long countUnreadInConversation(@Param("conv") Conversation conv, @Param("person") Person person);

    @Modifying
    @Query("UPDATE Message m SET m.readAt = CURRENT_TIMESTAMP " +
           "WHERE m.conversation = :conv AND m.sender <> :person AND m.readAt IS NULL")
    void markConversationRead(@Param("conv") Conversation conv, @Param("person") Person person);
}
