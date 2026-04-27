package rita.artha.shastra.controller;

import rita.artha.shastra.entity.Vehicle;
import rita.artha.shastra.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicle API", description = "Endpoints for managing vehicles")
public class VehicleController {
    private final VehicleService vehicleService;

    @GetMapping
    public List<Vehicle> getAllVehicles() {
        return vehicleService.getAllVehicles();
    }

    /**
     * Location-aware vehicle search.
     *
     * All parameters are optional. Typical usage from the mobile app:
     *   GET /api/v1/vehicles/search?country=India&page=0&size=20          (country-level)
     *   GET /api/v1/vehicles/search?country=India&city=Hyderabad           (city-level)
     *   GET /api/v1/vehicles/search?country=India&city=Hyderabad&neighbourhood=Banjara+Hills
     *
     * Exposed through the gateway at: GET /listings/search
     */
    @GetMapping("/search")
    @Operation(summary = "Search vehicles by location and optional filters")
    public ResponseEntity<Page<Vehicle>> searchVehicles(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String neighbourhood,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String subCategory,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                vehicleService.searchVehicles(country, state, city, neighbourhood, brand, subCategory, minPrice, maxPrice, page, size)
        );
    }

    @GetMapping("/{id}")
    public Optional<Vehicle> getVehicleById(@PathVariable Integer id) {
        return vehicleService.getVehicleById(id);
    }

    @PostMapping
    public Vehicle createVehicle(@RequestBody Vehicle vehicle) {
        return vehicleService.saveVehicle(vehicle);
    }

    @PutMapping("/{id}")
    public Vehicle updateVehicle(@PathVariable Integer id, @RequestBody Vehicle vehicle) {
        vehicle.setVehicleId(id);
        return vehicleService.saveVehicle(vehicle);
    }

    @DeleteMapping("/{id}")
    public void deleteVehicle(@PathVariable Integer id) {
        vehicleService.deleteVehicle(id);
    }
}
