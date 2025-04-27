package rita.artha.shastra.service;

import rita.artha.shastra.entity.Advertisement;
import rita.artha.shastra.repository.AdvertisementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdvertisementService {
    private final AdvertisementRepository advertisementRepository;

    public List<Advertisement> getAllAdvertisements() {
        return advertisementRepository.findAll();
    }

    public Optional<Advertisement> getAdvertisementById(Integer id) {
        return advertisementRepository.findById(id);
    }

    public Advertisement saveAdvertisement(Advertisement ad) {
        return advertisementRepository.save(ad);
    }

    public void deleteAdvertisement(Integer id) {
        advertisementRepository.deleteById(id);
    }
}
