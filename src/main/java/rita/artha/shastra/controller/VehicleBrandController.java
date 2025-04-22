package rita.artha.shastra.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rita.artha.shastra.entity.VehicleBrand;
import rita.artha.shastra.repository.VehicleBrandRepository;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle-brands")
@RequiredArgsConstructor
@Tag(name = "Vehicle Brands", description = "APIs for managing vehicle brands")
public class VehicleBrandController {

    private final VehicleBrandRepository brandRepository;

    @GetMapping
    @Operation(summary = "Get all vehicle brands", description = "Retrieve a list of all vehicle brands")
    public List<VehicleBrand> getAllBrands() {
        return brandRepository.findAll();
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get brands by category", description = "Retrieve vehicle brands filtered by category ID")
    public List<VehicleBrand> getBrandsByCategory(@PathVariable Integer categoryId) {
        return brandRepository.findByCategoryId(categoryId);
    }

    @PostMapping
    @Operation(summary = "Create a new vehicle brand", description = "Add a new brand entry for a vehicle category")
    public VehicleBrand createBrand(@RequestBody VehicleBrand brand) {
        return brandRepository.save(brand);
    }
}