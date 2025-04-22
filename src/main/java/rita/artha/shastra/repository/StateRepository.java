package rita.artha.shastra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rita.artha.shastra.entity.State;

import java.util.List;

@Repository
public interface StateRepository extends JpaRepository<State, Integer> {
    List<State> findByCountryId(Integer countryId);
}
