package rita.artha.shastra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rita.artha.shastra.entity.Conversation;
import rita.artha.shastra.entity.Person;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByVehicleSourceIdAndBuyer(String vehicleSourceId, Person buyer);

    List<Conversation> findByBuyerOrSellerOrderByLastMessageAtDesc(Person buyer, Person seller);

    List<Conversation> findByVehicleSourceIdOrderByLastMessageAtDesc(String vehicleSourceId);
}
