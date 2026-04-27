package rita.artha.shastra.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rita.artha.shastra.entity.VehicleCategory;
import rita.artha.shastra.repository.VehicleCategoryRepository;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle-categories")
@RequiredArgsConstructor
@Tag(name = "Vehicle Categories", description = "APIs for vehicle categories")
public class VehicleCategoryController {

    private final VehicleCategoryRepository categoryRepository;

    @GetMapping
    @Operation(summary = "Get all vehicle categories")
    public List<VehicleCategory> getAllCategories() {
        return categoryRepository.findAll();
    }
}
