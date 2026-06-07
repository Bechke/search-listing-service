package rita.artha.shastra.repository;

import rita.artha.shastra.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Integer> {
    Optional<Organization> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Organization> findByNameContainingIgnoreCase(String name);
}
