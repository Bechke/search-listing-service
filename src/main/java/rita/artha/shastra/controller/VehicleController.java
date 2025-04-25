package rita.artha.shastra.controller;

import rita.artha.shastra.entity.Vehicle;
import rita.artha.shastra.service.VehicleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
