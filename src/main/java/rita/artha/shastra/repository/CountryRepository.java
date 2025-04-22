package rita.artha.shastra.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import rita.artha.shastra.entity.Country;

@Repository
public interface CountryRepository extends JpaRepository<Country, Integer> {}
