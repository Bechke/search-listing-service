package rita.artha.shastra.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import rita.artha.shastra.dto.VehicleDTO;
import io.micrometer.tracing.Tracer;
@Service
@RequiredArgsConstructor
public class VehicleProducerService {

    private final KafkaTemplate<String, VehicleDTO> kafkaTemplate;
    private final Tracer tracer;

    public void sendVehicleAd(VehicleDTO vehicleDTO) {
        tracer.currentSpan().tag("personId", vehicleDTO.getPersonId() != null ? vehicleDTO.getPersonId().toString() : "new");
        kafkaTemplate.send("vehicle-ads", vehicleDTO);
    }
}
