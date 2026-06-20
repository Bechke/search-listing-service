package rita.artha.shastra.controller;

import rita.artha.shastra.entity.Advertisement;
import rita.artha.shastra.repository.AdvertisementRepository;
import rita.artha.shastra.service.AdvertisementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/ads")
@RequiredArgsConstructor
@Tag(name = "Advertisement API", description = "Endpoints for managing advertisements")
public class AdvertisementController {
    private final AdvertisementService  advertisementService;
    private final AdvertisementRepository advertisementRepository;

    @GetMapping("/search")
    public Page<Advertisement> search(
            @RequestParam String country,
            @RequestParam String state,
            @RequestParam String city,
            @RequestParam(required = false) String neighbourhood,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return advertisementService.search(country, state, city, neighbourhood, PageRequest.of(page, size));
    }

    @GetMapping
    public List<Advertisement> getAllAds() {
        return advertisementService.getAllAdvertisements();
    }

    /**
     * Returns only the ads belonging to the currently authenticated user.
     * X-User-Id is injected by the gateway's UserIdHeaderFilter from the JWT sub claim.
     */
    @GetMapping("/my")
    @Operation(summary = "Get current user's ads")
    public List<Advertisement> getMyAds(
            @RequestHeader(value = "X-User-Id", required = false) String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) return List.of();
        return advertisementRepository.findByPerson_KeycloakId(keycloakId);
    }

    @GetMapping("/org/{orgId}")
    @Operation(summary = "Get all ads for an organization (public)")
    public List<Advertisement> getOrgAds(@PathVariable Integer orgId) {
        return advertisementRepository.findByOrganization_Id(orgId);
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
