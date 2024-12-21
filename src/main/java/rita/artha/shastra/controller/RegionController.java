package rita.artha.shastra.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import rita.artha.shastra.entity.Region;
import rita.artha.shastra.service.RegionService;


import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/regions")
public class RegionController {

    @Autowired
    private RegionService regionService;

    @GetMapping
    public List<Region> getAllRegions() {
        return regionService.getAllRegions();
    }

    @GetMapping("/{id}")
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