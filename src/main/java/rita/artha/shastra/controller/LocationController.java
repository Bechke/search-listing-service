package rita.artha.shastra.controller;

import rita.artha.shastra.entity.*;
import rita.artha.shastra.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import rita.artha.shastra.service.LocationService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Location Data", description = "APIs for managing country, state, city, and neighbourhood data")
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/countries")
    @Operation(summary = "Get all countries")
    public List<Country> getCountries() {
        return locationService.getAllCountries();
    }

    @GetMapping("/states/{countryId}")
    @Operation(summary = "Get states by country ID")
    public List<State> getStatesByCountry(@PathVariable Integer countryId) {
        return locationService.getStatesByCountry(countryId);
    }

    @GetMapping("/cities/{stateId}")
    @Operation(summary = "Get cities by state ID")
    public List<City> getCitiesByState(@PathVariable Integer stateId) {
        return locationService.getCitiesByState(stateId);
    }

    @GetMapping("/neighbourhoods/{cityId}")
    @Operation(summary = "Get neighbourhoods by city ID")
    public List<Neighbourhood> getNeighbourhoodsByCity(@PathVariable Integer cityId) {
        return locationService.getNeighbourhoodsByCity(cityId);
    }
}
