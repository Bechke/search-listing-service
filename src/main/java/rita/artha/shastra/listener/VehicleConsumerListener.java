package rita.artha.shastra.listener;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import rita.artha.shastra.dto.VehicleDTO;
import rita.artha.shastra.entity.Person;
import rita.artha.shastra.entity.Vehicle;
import rita.artha.shastra.repository.VehicleRepository;
import rita.artha.shastra.service.PersonService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@KafkaListener(topics = "vehicle-ads", groupId = "vehicle-ads-consumer-group")
public class VehicleConsumerListener {

    private final VehicleRepository vehicleRepository;
    private final PersonService personService;
    private static final Logger logger = LoggerFactory.getLogger(VehicleConsumerListener.class);

    @KafkaHandler
    public void consumeVehicleAd(VehicleDTO vehicleDTO) {
        logger.info("Consumed vehicle ad: {}", vehicleDTO);

        if (vehicleDTO.getPersonId() != null) {
            Optional<Person> personOptional = personService.getPersonById(vehicleDTO.getPersonId());

            if (personOptional.isPresent()) {
                Vehicle vehicle = mapToEntity(vehicleDTO, personOptional.get());
                vehicleRepository.save(vehicle);
                logger.info("Saved vehicle: {}", vehicle);
            } else {
                logger.error("Person with ID {} not found, skipping vehicle save", vehicleDTO.getPersonId());
            }
        } else {
            logger.error("Invalid vehicle DTO: Person ID is null");
        }
    }

    private Vehicle mapToEntity(VehicleDTO vehicleDTO, Person person) {
        return Vehicle.builder()
                .person(person)
                .mobile(vehicleDTO.getMobile())
                .adSubcategory(vehicleDTO.getAdSubcategory())
                .brand(vehicleDTO.getBrand())
                .year(vehicleDTO.getYear())
                .fuelType(vehicleDTO.getFuelType())
                .transmission(vehicleDTO.getTransmission())
                .odometerReading(vehicleDTO.getOdometerReading())
                .numOwners(vehicleDTO.getNumOwners())
                .title(vehicleDTO.getTitle())
                .description(vehicleDTO.getDescription())
                .price(vehicleDTO.getPrice())
                .defaultImgPath(vehicleDTO.getDefaultImgPath())
                .country(vehicleDTO.getCountry())
                .state(vehicleDTO.getState())
                .city(vehicleDTO.getCity())
                .neighbourhood(vehicleDTO.getNeighbourhood())
                .status(vehicleDTO.getStatus())
                .createdAt(vehicleDTO.getCreatedAt())
                .updatedAt(vehicleDTO.getUpdatedAt())
                .build();
    }
}
