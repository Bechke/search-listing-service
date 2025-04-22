package rita.artha.shastra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rita.artha.shastra.entity.Neighbourhood;

import java.util.List;

@Repository
public interface NeighbourhoodRepository extends JpaRepository<Neighbourhood, Integer> {
    List<Neighbourhood> findByCityId(Integer cityId);
}
