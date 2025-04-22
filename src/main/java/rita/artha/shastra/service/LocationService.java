package rita.artha.shastra.service;

import rita.artha.shastra.entity.*;
import rita.artha.shastra.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final CountryRepository countryRepository;
    private final StateRepository stateRepository;
    private final CityRepository cityRepository;
    private final NeighbourhoodRepository neighbourhoodRepository;

    public List<Country> getAllCountries() {
        return countryRepository.findAll();
    }

    public List<State> getStatesByCountry(Integer countryId) {
        return stateRepository.findByCountryId(countryId);
    }

    public List<City> getCitiesByState(Integer stateId) {
        return cityRepository.findByStateId(stateId);
    }

    public List<Neighbourhood> getNeighbourhoodsByCity(Integer cityId) {
        return neighbourhoodRepository.findByCityId(cityId);
    }
}
