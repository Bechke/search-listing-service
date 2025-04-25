package rita.artha.shastra.controller;

import rita.artha.shastra.entity.Advertisement;
import rita.artha.shastra.service.AdvertisementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
