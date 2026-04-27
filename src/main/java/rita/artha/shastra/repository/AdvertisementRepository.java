package rita.artha.shastra.repository;

import rita.artha.shastra.entity.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Integer> {
    Optional<Advertisement> findByVehicleSourceId(String vehicleSourceId);
    List<Advertisement> findByPerson_KeycloakId(String keycloakId);
}
