package rita.artha.shastra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rita.artha.shastra.entity.VehicleCategory;

@Repository
public interface VehicleCategoryRepository extends JpaRepository<VehicleCategory, Integer> {
}