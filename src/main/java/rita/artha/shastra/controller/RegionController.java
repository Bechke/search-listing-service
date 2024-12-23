package rita.artha.shastra.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import rita.artha.shastra.entity.Region;
import rita.artha.shastra.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/regions")
@Tag(name = "Region", description = "Manage regions in the application")
public class RegionController {

    @Autowired
    private RegionService regionService;

    @GetMapping
    public List<Region> getAllRegions() {
        return regionService.getAllRegions();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get Region by ID", description = "Retrieve a specific region by its unique ID")
    public Optional<Region> getRegionById(@PathVariable Long id) {
        return regionService.getRegionById(id);
    }

    @PostMapping
    public Region createOrUpdateRegion(@RequestBody Region region) {
        return regionService.saveRegion(region);
    }

    @DeleteMapping("/{id}")
    public void deleteRegion(@PathVariable Long id) {
        regionService.deleteRegion(id);
    }
}