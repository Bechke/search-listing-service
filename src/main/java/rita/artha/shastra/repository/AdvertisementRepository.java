package rita.artha.shastra.repository;

import rita.artha.shastra.entity.Advertisement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Integer> {
    Optional<Advertisement> findByVehicleSourceId(String vehicleSourceId);
    List<Advertisement> findByPerson_KeycloakId(String keycloakId);
    List<Advertisement> findByPerson_KeycloakIdAndStatus(String keycloakId, String status);
    List<Advertisement> findByOrganization_Id(Integer orgId);
    List<Advertisement> findByOrganization_IdAndStatus(Integer orgId, String status);

    Page<Advertisement> findByCountryAndStateAndCityAndNeighbourhood(
            String country, String state, String city, String neighbourhood, Pageable pageable);

    Page<Advertisement> findByCountryAndStateAndCity(
            String country, String state, String city, Pageable pageable);

    /** Used by the admin panel to fetch listings by status, e.g. PENDING_REVIEW */
    Page<Advertisement> findByStatus(String status, Pageable pageable);

    /**
     * Counts active listings for a seller.
     * Used by the listing-limit enforcement check before allowing a new post.
     * Statuses REJECTED and SOLD are excluded — they no longer consume a slot.
     */
    long countByPerson_KeycloakIdAndStatusNotIn(String keycloakId, java.util.List<String> excludedStatuses);

    /** Same as above, scoped to an organization's listings for org quota checks. */
    long countByOrganization_IdAndStatusNotIn(Integer organizationId, java.util.List<String> excludedStatuses);
}
