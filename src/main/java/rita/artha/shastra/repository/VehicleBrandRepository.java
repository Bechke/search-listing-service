package rita.artha.shastra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rita.artha.shastra.entity.VehicleBrand;
import java.util.List;

@Repository
public interface VehicleBrandRepository extends JpaRepository<VehicleBrand, Integer> {
    List<VehicleBrand> findByCategoryId(Integer categoryId);
}
