package rita.artha.shastra.service;

import rita.artha.shastra.entity.Advertisement;
import rita.artha.shastra.repository.AdvertisementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdvertisementService {
    private final AdvertisementRepository advertisementRepository;

    public List<Advertisement> getAllAdvertisements() {
        return advertisementRepository.findAll();
    }
}
