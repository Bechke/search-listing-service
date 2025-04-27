package rita.artha.shastra.controller;

import rita.artha.shastra.entity.Advertisement;
import rita.artha.shastra.service.AdvertisementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/ads")
@RequiredArgsConstructor
@Tag(name = "Advertisement API", description = "Endpoints for managing advertisements")
public class AdvertisementController {
    private final AdvertisementService advertisementService;

    @GetMapping
    public List<Advertisement> getAllAds() {
        return advertisementService.getAllAdvertisements();
    }

    @GetMapping("/{id}")
    public Optional<Advertisement> getAdById(@PathVariable Integer id) {
        return advertisementService.getAdvertisementById(id);
    }

    @PostMapping
    public Advertisement createAd(@RequestBody Advertisement ad) {
        return advertisementService.saveAdvertisement(ad);
    }

    @PutMapping("/{id}")
    public Advertisement updateAd(@PathVariable Integer id, @RequestBody Advertisement ad) {
        ad.setAdvertisementId(id);
        return advertisementService.saveAdvertisement(ad);
    }

    @DeleteMapping("/{id}")
    public void deleteAd(@PathVariable Integer id) {
        advertisementService.deleteAdvertisement(id);
    }
}
