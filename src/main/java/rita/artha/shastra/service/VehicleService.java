package rita.artha.shastra.service;

import rita.artha.shastra.entity.Vehicle;
import rita.artha.shastra.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VehicleService {
    private final VehicleRepository vehicleRepository;

    /** Public browse — only ACTIVE listings. */
    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findByStatus("ACTIVE");
    }

    public Optional<Vehicle> getVehicleById(Integer id) {
        return vehicleRepository.findById(id);
    }

    public Vehicle saveVehicle(Vehicle vehicle) {
        return vehicleRepository.save(vehicle);
    }

    public void deleteVehicle(Integer id) {
        vehicleRepository.deleteById(id);
    }

    /**
     * Location-aware search. All parameters are optional.
     * Passing country without city returns all listings in that country.
     * Omitting country returns listings from all countries.
     */
    public Page<Vehicle> searchVehicles(
            String country,
            String state,
            String city,
            String neighbourhood,
            String brand,
            String subCategory,
            Double minPrice,
            Double maxPrice,
            int page,
            int size
    ) {
        return vehicleRepository.searchVehicles(
                country, state, city, neighbourhood, brand, subCategory, minPrice, maxPrice,
                PageRequest.of(page, size)
        );
    }

    /** Currently-boosted listings for the "Featured" rail, optionally scoped to a subCategory. */
    public Page<Vehicle> getFeaturedVehicles(String subCategory, int limit) {
        return vehicleRepository.findFeatured(subCategory, PageRequest.of(0, limit));
    }
}
