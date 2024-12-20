package rita.artha.shastra.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rita.artha.shastra.entity.Region;
import rita.artha.shastra.repository.RegionRepository;


import java.util.List;
import java.util.Optional;

@Service
public class RegionService {

    @Autowired
    private RegionRepository regionRepository;

    // Create or Update Region
    public Region saveRegion(Region region) {
        return regionRepository.save(region);
    }

    // Get all Regions
    public List<Region> getAllRegions() {
        return regionRepository.findAll();
    }

    // Get Region by ID
    public Optional<Region> getRegionById(Long regionId) {
        return regionRepository.findById(regionId);
    }

    // Delete Region by ID
    public void deleteRegion(Long regionId) {
        regionRepository.deleteById(regionId);
    }
}