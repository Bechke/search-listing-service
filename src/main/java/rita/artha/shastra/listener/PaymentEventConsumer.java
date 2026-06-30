package rita.artha.shastra.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rita.artha.shastra.dto.PaymentEvent;
import rita.artha.shastra.repository.AdvertisementRepository;
import rita.artha.shastra.repository.VehicleRepository;

import java.time.LocalDateTime;

/**
 * Listens on the "payment-events" topic.
 *
 * When a PAYMENT_SUCCESSFUL event arrives the corresponding Advertisement and
 * Vehicle rows move from their current status (e.g. DRAFT / PENDING_PAYMENT)
 * to PENDING_REVIEW so an admin can approve and make the listing live.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventConsumer {

    private final AdvertisementRepository advertisementRepository;
    private final VehicleRepository       vehicleRepository;

    @Transactional
    @KafkaListener(topics = "payment-events", groupId = "search-listing-payment-group")
    public void consume(PaymentEvent event) {
        if (event == null || event.getListingId() == null) {
            log.warn("PaymentEventConsumer: received null or incomplete event, skipping");
            return;
        }

        log.info("PaymentEventConsumer: eventType={} listingId={} sellerId={}",
                event.getEventType(), event.getListingId(), event.getSellerId());

        if (!"PAYMENT_SUCCESSFUL".equals(event.getEventType())) {
            log.debug("PaymentEventConsumer: ignoring eventType={}", event.getEventType());
            return;
        }

        String listingId = event.getListingId();

        // Update Advertisement row
        advertisementRepository.findByVehicleSourceId(listingId).ifPresentOrElse(ad -> {
            ad.setStatus("PENDING_REVIEW");
            ad.setUpdatedAt(LocalDateTime.now());
            advertisementRepository.save(ad);
            log.info("Advertisement {} status → PENDING_REVIEW", listingId);
        }, () -> log.warn("PaymentEventConsumer: no Advertisement found for listingId={}", listingId));

        // Update Vehicle row
        vehicleRepository.findByVehicleSourceId(listingId).ifPresentOrElse(vehicle -> {
            vehicle.setStatus("PENDING_REVIEW");
            vehicle.setUpdatedAt(LocalDateTime.now());
            vehicleRepository.save(vehicle);
            log.info("Vehicle {} status → PENDING_REVIEW", listingId);
        }, () -> log.warn("PaymentEventConsumer: no Vehicle found for listingId={}", listingId));
    }
}
