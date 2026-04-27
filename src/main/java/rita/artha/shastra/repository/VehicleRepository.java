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

    @Query("""
            SELECT v FROM Vehicle v
            WHERE v.status != 'DELETED'
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
}
