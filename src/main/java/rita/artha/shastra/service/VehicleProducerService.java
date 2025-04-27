package rita.artha.shastra.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import rita.artha.shastra.dto.VehicleDTO;

@Service
@RequiredArgsConstructor
public class VehicleProducerService {

    private final KafkaTemplate<String, VehicleDTO> kafkaTemplate;

    public void sendVehicleAd(VehicleDTO vehicleDTO) {
        kafkaTemplate.send("vehicle-ads", vehicleDTO);
    }
}
