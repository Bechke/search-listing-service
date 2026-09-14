package rita.artha.shastra.repository;

import rita.artha.shastra.entity.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Integer> {

    Optional<Vehicle> findByVehicleSourceId(String vehicleSourceId);

    /** Batch price lookup for admin-web's listing summary — one query, not N+1. */
    java.util.List<Vehicle> findByVehicleSourceIdIn(java.util.List<String> vehicleSourceIds);

    /** Public browse endpoint — only ACTIVE listings are visible to guests/buyers. */
    Page<Vehicle> findByStatus(String status, Pageable pageable);
    java.util.List<Vehicle> findByStatus(String status);

    @Query("""
            SELECT v FROM Vehicle v
            WHERE v.status = 'ACTIVE'
            AND (:country       IS NULL OR LOWER(v.country)       = LOWER(:country))
            AND (:state         IS NULL OR LOWER(v.state)         = LOWER(:state))
            AND (:city          IS NULL OR LOWER(v.city)          = LOWER(:city))
            AND (:neighbourhood IS NULL OR LOWER(v.neighbourhood) = LOWER(:neighbourhood))
            AND (:brand         IS NULL OR LOWER(v.brand)         LIKE LOWER(CONCAT('%', :brand, '%')))
            AND (:subCategory   IS NULL OR v.adSubcategory        = :subCategory)
            AND (:minPrice      IS NULL OR v.price               >= :minPrice)
            AND (:maxPrice      IS NULL OR v.price               <= :maxPrice)
            ORDER BY v.createdAt DESC
            """)
    Page<Vehicle> searchVehicles(
            @Param("country")       String country,
            @Param("state")         String state,
            @Param("city")          String city,
            @Param("neighbourhood") String neighbourhood,
            @Param("brand")         String brand,
            @Param("subCategory")   String subCategory,
            @Param("minPrice")      Double minPrice,
            @Param("maxPrice")      Double maxPrice,
            Pageable pageable
    );

    /**
     * Currently-boosted listings for the "Featured" rail — Vehicle carries no boost
     * fields itself, so this correlates to Advertisement by vehicleSourceId (the two
     * aren't JPA-mapped to each other, just share that string key). "Currently
     * featured" is computed here, not stored — see PlanLimits' concurrency-cap note.
     */
    @Query("""
            SELECT v FROM Vehicle v, Advertisement a
            WHERE v.vehicleSourceId = a.vehicleSourceId
            AND v.status = 'ACTIVE'
            AND a.boosted = true
            AND a.boostedUntil > CURRENT_TIMESTAMP
            AND (:subCategory IS NULL OR v.adSubcategory = :subCategory)
            ORDER BY a.boostedUntil DESC
            """)
    Page<Vehicle> findFeatured(@Param("subCategory") String subCategory, Pageable pageable);
}
