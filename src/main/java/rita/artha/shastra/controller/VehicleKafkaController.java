package rita.artha.shastra.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import rita.artha.shastra.dto.VehicleDTO;
import rita.artha.shastra.entity.Person;
import rita.artha.shastra.entity.Vehicle;
import rita.artha.shastra.service.PersonService;
import rita.artha.shastra.service.VehicleProducerService;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/vehicle-kafka")
@RequiredArgsConstructor
@Tag(name = "Vehicle Kafka Producer")
public class VehicleKafkaController {

    private final VehicleProducerService producerService;
    private final PersonService personService;
    private static final Logger logger = LoggerFactory.getLogger(VehicleKafkaController.class);

    @Operation(summary = "Send vehicle ad to Kafka topic")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vehicle ad sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input")
    })
    @PostMapping
    public ResponseEntity<String> sendVehicleAd(
            @Parameter(description = "Vehicle details to send", required = true)
            @RequestBody VehicleDTO vehicleDTO) {

        logger.info("Sending vehicle ad to Kafka topic: {}", vehicleDTO);
        producerService.sendVehicleAd(vehicleDTO);
        return ResponseEntity.ok("Vehicle ad sent to Kafka topic");
    }
}
