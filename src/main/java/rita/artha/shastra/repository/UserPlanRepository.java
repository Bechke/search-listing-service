package rita.artha.shastra.repository;

import rita.artha.shastra.entity.UserPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPlanRepository extends JpaRepository<UserPlan, Long> {

    Optional<UserPlan> findByKeycloakId(String keycloakId);
}
