package rita.artha.shastra.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rita.artha.shastra.dto.VehicleDTO;
import rita.artha.shastra.entity.Advertisement;
import rita.artha.shastra.entity.Vehicle;
import rita.artha.shastra.repository.AdvertisementRepository;
import rita.artha.shastra.repository.VehicleRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin operations: bulk approve and bulk reject listings.
 *
 * Both operations:
 *  1. Update the Advertisement and Vehicle rows in the local DB.
 *  2. Publish an UPDATE event on vehicle-ads so notification-service sends the
 *     appropriate push/email to the seller (LISTING_APPROVED / LISTING_REJECTED).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdvertisementRepository           advertisementRepository;
    private final VehicleRepository                 vehicleRepository;
    private final KafkaTemplate<String, VehicleDTO> kafkaTemplate;

    /**
     * Approve a batch of listings — sets status to ACTIVE.
     *
     * @param listingIds vehicleSourceId values to approve
     * @return number of listings successfully updated
     */
    @Transactional
    public int bulkApprove(List<String> listingIds) {
        int count = 0;
        for (String id : listingIds) {
            count += updateStatus(id, "ACTIVE", null);
        }
        log.info("Admin bulkApprove: {} / {} listings approved", count, listingIds.size());
        return count;
    }

    /**
     * Reject a batch of listings — sets status to REJECTED.
     *
     * @param listingIds vehicleSourceId values to reject
     * @param reason     optional rejection reason forwarded in the Kafka event
     * @return number of listings successfully updated
     */
    @Transactional
    public int bulkReject(List<String> listingIds, String reason) {
        int count = 0;
        for (String id : listingIds) {
            count += updateStatus(id, "REJECTED", reason);
        }
        log.info("Admin bulkReject: {} / {} listings rejected", count, listingIds.size());
        return count;
    }

    // ── private helpers ───────────────────────────────────────────────────────

    /**
     * Updates one listing's status in both Advertisement and Vehicle tables,
     * then publishes a vehicle-ads UPDATE event so notification-service reacts.
     *
     * @return 1 if found and updated, 0 if listing not found
     */
    private int updateStatus(String vehicleSourceId, String status, String reason) {
        Advertisement ad = advertisementRepository.findByVehicleSourceId(vehicleSourceId)
                .orElse(null);
        Vehicle vehicle  = vehicleRepository.findByVehicleSourceId(vehicleSourceId)
                .orElse(null);

        if (ad == null && vehicle == null) {
            log.warn("Admin updateStatus: listingId={} not found, skipping", vehicleSourceId);
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();

        if (ad != null) {
            ad.setStatus(status);
            ad.setUpdatedAt(now);
            advertisementRepository.save(ad);
        }

        if (vehicle != null) {
            vehicle.setStatus(status);
            vehicle.setUpdatedAt(now);
            vehicleRepository.save(vehicle);
        }

        // Build a minimal VehicleDTO UPDATE event for notification-service
        VehicleDTO event = buildUpdateEvent(vehicleSourceId, status, ad, vehicle, reason);
        kafkaTemplate.send("vehicle-ads", vehicleSourceId, event);
        log.info("Published vehicle-ads UPDATE event: listingId={} status={}", vehicleSourceId, status);

        return 1;
    }

    private VehicleDTO buildUpdateEvent(String vehicleSourceId, String status,
                                        Advertisement ad, Vehicle vehicle, String reason) {
        VehicleDTO dto = new VehicleDTO();
        dto.setEventType("UPDATE");
        dto.setVehicleId(vehicleSourceId);
        dto.setStatus(status);

        // Populate seller info so notification-service can send push / email
        if (ad != null && ad.getPerson() != null) {
            dto.setSellerId(ad.getPerson().getKeycloakId());
            dto.setSellerEmail(ad.getPerson().getEmail());
        } else if (vehicle != null && vehicle.getPerson() != null) {
            dto.setSellerId(vehicle.getPerson().getKeycloakId());
        }

        if (ad != null) {
            dto.setTitle(ad.getTitle());
            dto.setAdSubcategory(ad.getAdSubcategory());
            dto.setDefaultImgPath(ad.getDefaultImgPath());
            dto.setCountry(ad.getCountry());
            dto.setState(ad.getState());
            dto.setCity(ad.getCity());
            dto.setNeighbourhood(ad.getNeighbourhood());
        }

        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }
}
