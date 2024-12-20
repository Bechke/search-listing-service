package rita.artha.shastra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rita.artha.shastra.entity.Region;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {
    // Custom queries can be added here if needed
}