package rita.artha.shastra.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import rita.artha.shastra.dto.VehicleDTO;
import rita.artha.shastra.entity.Advertisement;
import rita.artha.shastra.entity.Person;
import rita.artha.shastra.entity.Vehicle;
import rita.artha.shastra.repository.AdvertisementRepository;
import rita.artha.shastra.repository.OrganizationRepository;
import rita.artha.shastra.repository.PersonRepository;
import rita.artha.shastra.repository.VehicleRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@KafkaListener(topics = "vehicle-ads", groupId = "vehicle-ads-consumer-group")
public class VehicleConsumerListener {

    private final VehicleRepository        vehicleRepository;
    private final PersonRepository         personRepository;
    private final AdvertisementRepository  advertisementRepository;
    private final OrganizationRepository   orgRepo;
    private final ObjectMapper             objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(VehicleConsumerListener.class);

    @KafkaHandler
    public void consumeVehicleAd(VehicleDTO dto) {
        logger.info("Received vehicle-ads event: type={} vehicleId={}", dto.getEventType(), dto.getVehicleId());

        if (dto.getSellerId() == null && dto.getPersonId() == null) {
            logger.error("Skipping event — no sellerId or personId present");
            return;
        }

        String eventType = dto.getEventType() != null ? dto.getEventType() : "CREATE";

        if ("DELETE".equals(eventType)) {
            handleDelete(dto);
            return;
        }

        // ── 1. Find-or-create the seller (Person) by Keycloak sub ─────────────
        Person person = resolvePerson(dto);

        // ── 2. Upsert Vehicle row ──────────────────────────────────────────────
        Vehicle vehicle = vehicleRepository
                .findByVehicleSourceId(dto.getVehicleId())
                .orElse(new Vehicle());

        vehicle.setVehicleSourceId(dto.getVehicleId());
        vehicle.setPerson(person);
        vehicle.setAdSubcategory(dto.getAdSubcategory());
        vehicle.setBrand(dto.getBrand());
        vehicle.setYear(dto.getYear());
        vehicle.setFuelType(dto.getFuelType());
        vehicle.setTransmission(dto.getTransmission());
        vehicle.setOdometerReading(dto.getOdometerReading());
        vehicle.setNumOwners(dto.getNumOwners());
        vehicle.setTitle(dto.getTitle());
        vehicle.setDescription(dto.getDescription());
        vehicle.setPrice(dto.getPrice());
        vehicle.setDefaultImgPath(dto.getDefaultImgPath());
        vehicle.setCountry(dto.getCountry());
        vehicle.setState(dto.getState());
        vehicle.setCity(dto.getCity() != null ? dto.getCity() : dto.getNeighbourhood());
        vehicle.setNeighbourhood(dto.getNeighbourhood());
        vehicle.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        vehicle.setUpdatedAt(dto.getUpdatedAt() != null ? dto.getUpdatedAt() : LocalDateTime.now());
        if (vehicle.getCreatedAt() == null) {
            vehicle.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now());
        }

        if (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) {
            try {
                vehicle.setImageUrlsJson(objectMapper.writeValueAsString(dto.getImageUrls()));
            } catch (JsonProcessingException e) {
                logger.warn("Could not serialize imageUrls: {}", e.getMessage());
            }
        }

        if (dto.getOrganizationId() != null) {
            try {
                orgRepo.findById(Integer.parseInt(dto.getOrganizationId()))
                       .ifPresent(vehicle::setOrganization);
            } catch (NumberFormatException ignored) {
                logger.warn("Invalid organizationId in VehicleDTO: {}", dto.getOrganizationId());
            }
        }

        vehicleRepository.save(vehicle);
        logger.info("Saved/updated Vehicle for vehicleSourceId={}", dto.getVehicleId());

        // ── 3. Upsert Advertisement row ────────────────────────────────────────
        Advertisement ad = advertisementRepository
                .findByVehicleSourceId(dto.getVehicleId())
                .orElse(new Advertisement());

        ad.setVehicleSourceId(dto.getVehicleId());
        ad.setPerson(person);
        ad.setAdCategory("VEHICLE");
        ad.setAdSubcategory(dto.getAdSubcategory());
        ad.setTitle(dto.getTitle());
        ad.setDefaultImgPath(dto.getDefaultImgPath());
        ad.setCountry(dto.getCountry());
        ad.setState(dto.getState());
        ad.setCity(dto.getCity() != null ? dto.getCity() : dto.getNeighbourhood());
        ad.setNeighbourhood(dto.getNeighbourhood());
        ad.setStatus(dto.getStatus() != null ? dto.getStatus() : "ACTIVE");
        ad.setUpdatedAt(dto.getUpdatedAt() != null ? dto.getUpdatedAt() : LocalDateTime.now());
        if (ad.getCreatedAt() == null) {
            ad.setCreatedAt(dto.getCreatedAt() != null ? dto.getCreatedAt() : LocalDateTime.now());
        }

        if (dto.getOrganizationId() != null) {
            try {
                orgRepo.findById(Integer.parseInt(dto.getOrganizationId()))
                       .ifPresent(ad::setOrganization);
            } catch (NumberFormatException ignored) {
                logger.warn("Invalid organizationId in VehicleDTO: {}", dto.getOrganizationId());
            }
        }

        advertisementRepository.save(ad);
        logger.info("Saved/updated Advertisement for vehicleSourceId={}", dto.getVehicleId());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void handleDelete(VehicleDTO dto) {
        vehicleRepository.findByVehicleSourceId(dto.getVehicleId()).ifPresent(v -> {
            v.setStatus("DELETED");
            vehicleRepository.save(v);
            logger.info("Soft-deleted Vehicle vehicleSourceId={}", dto.getVehicleId());
        });
        advertisementRepository.findByVehicleSourceId(dto.getVehicleId()).ifPresent(a -> {
            a.setStatus("DELETED");
            advertisementRepository.save(a);
            logger.info("Soft-deleted Advertisement vehicleSourceId={}", dto.getVehicleId());
        });
    }

    private Person resolvePerson(VehicleDTO dto) {
        if (dto.getSellerId() != null) {
            return personRepository.findByKeycloakId(dto.getSellerId())
                    .orElseGet(() -> {
                        Person p = new Person();
                        p.setKeycloakId(dto.getSellerId());
                        p.setEmail(dto.getSellerEmail());
                        p.setFullName("");
                        p.setMobileNumber("");
                        p.setCreatedAt(LocalDateTime.now());
                        p.setUpdatedAt(LocalDateTime.now());
                        return personRepository.save(p);
                    });
        }

        return personRepository.findById(dto.getPersonId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Person not found for personId=" + dto.getPersonId()));
    }
}
