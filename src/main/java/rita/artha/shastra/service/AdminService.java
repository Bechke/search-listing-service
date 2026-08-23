package rita.artha.shastra.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    private final ObjectMapper                      objectMapper;

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

    /**
     * Called by PaymentEventConsumer after a seller's plan is upgraded — releases
     * any listings blocked at PENDING_PAYMENT back into the admin review queue.
     * Not a full approval: the admin still has to review them, same as any other
     * PENDING_REVIEW listing.
     */
    @Transactional
    public int releaseFromPendingPayment(String keycloakId) {
        List<Advertisement> blocked = advertisementRepository
                .findByPerson_KeycloakIdAndStatus(keycloakId, "PENDING_PAYMENT");

        int count = 0;
        for (Advertisement ad : blocked) {
            count += updateStatus(ad.getVehicleSourceId(), "PENDING_REVIEW", null);
        }
        if (count > 0) {
            log.info("Released {} listing(s) from PENDING_PAYMENT to PENDING_REVIEW for seller {}", count, keycloakId);
        }
        return count;
    }

    /** Same as releaseFromPendingPayment, but for an organization's quota pool. */
    @Transactional
    public int releaseOrgFromPendingPayment(Integer organizationId) {
        List<Advertisement> blocked = advertisementRepository
                .findByOrganization_IdAndStatus(organizationId, "PENDING_PAYMENT");

        int count = 0;
        for (Advertisement ad : blocked) {
            count += updateStatus(ad.getVehicleSourceId(), "PENDING_REVIEW", null);
        }
        if (count > 0) {
            log.info("Released {} listing(s) from PENDING_PAYMENT to PENDING_REVIEW for org {}", count, organizationId);
        }
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
            // REJECTED carries the reason forward; any other transition (approve) clears it.
            ad.setRejectionReason("REJECTED".equals(status) ? reason : null);
            ad.setUpdatedAt(now);
            advertisementRepository.save(ad);
        }

        if (vehicle != null) {
            vehicle.setStatus(status);
            vehicle.setUpdatedAt(now);
            vehicleRepository.save(vehicle);
        }

        // Republish the FULL listing (not just status) so VehicleConsumerListener's
        // upsert doesn't null out brand/year/price/etc. when it re-consumes this event.
        VehicleDTO event = buildUpdateEvent(vehicleSourceId, status, ad, vehicle, reason);
        publishAfterCommit(vehicleSourceId, status, event);

        return 1;
    }

    /**
     * Defers the Kafka publish until this transaction actually commits.
     *
     * VehicleConsumerListener re-consumes this same "vehicle-ads" topic, and for a
     * PENDING_REVIEW status it re-runs the quota check (AdvertisementService.resolveIntakeStatus),
     * which re-queries the seller's UserPlan and active-listing count. If the event went out
     * synchronously mid-transaction (kafkaTemplate.send() has no ties to the JPA transaction —
     * there's no Kafka transaction manager wired up), that re-consume can race ahead of this
     * transaction's commit and read the pre-commit plan/status under READ_COMMITTED — observed
     * concretely as releaseFromPendingPayment's PENDING_REVIEW write getting silently flipped
     * straight back to PENDING_PAYMENT a few hundred ms later, because the listener still saw
     * the old (lower) plan limit. Publishing only after commit closes that window.
     */
    private void publishAfterCommit(String vehicleSourceId, String status, VehicleDTO event) {
        Runnable publish = () -> {
            kafkaTemplate.send("vehicle-ads", vehicleSourceId, event);
            log.info("Published vehicle-ads UPDATE event: listingId={} status={}", vehicleSourceId, status);
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
        } else {
            // No open transaction (shouldn't happen given every caller is @Transactional) —
            // fall back to publishing immediately rather than silently dropping the event.
            publish.run();
        }
    }

    private VehicleDTO buildUpdateEvent(String vehicleSourceId, String status,
                                        Advertisement ad, Vehicle vehicle, String reason) {
        VehicleDTO dto = new VehicleDTO();
        dto.setEventType("UPDATE");
        dto.setVehicleId(vehicleSourceId);
        dto.setStatus(status);
        dto.setRejectionReason("REJECTED".equals(status) ? reason : null);

        // Populate seller info so notification-service can send push / email
        if (ad != null && ad.getPerson() != null) {
            dto.setSellerId(ad.getPerson().getKeycloakId());
            dto.setSellerEmail(ad.getPerson().getEmail());
        } else if (vehicle != null && vehicle.getPerson() != null) {
            dto.setSellerId(vehicle.getPerson().getKeycloakId());
        }

        // Prefer the Vehicle row for the full field set — it carries everything
        // Advertisement doesn't (brand/year/price/fuelType/transmission/etc).
        if (vehicle != null) {
            dto.setTitle(vehicle.getTitle());
            dto.setDescription(vehicle.getDescription());
            dto.setPrice(vehicle.getPrice());
            dto.setAdSubcategory(vehicle.getAdSubcategory());
            dto.setBrand(vehicle.getBrand());
            dto.setYear(vehicle.getYear());
            dto.setFuelType(vehicle.getFuelType());
            dto.setTransmission(vehicle.getTransmission());
            dto.setOdometerReading(vehicle.getOdometerReading());
            dto.setNumOwners(vehicle.getNumOwners());
            dto.setDefaultImgPath(vehicle.getDefaultImgPath());
            dto.setCountry(vehicle.getCountry());
            dto.setState(vehicle.getState());
            dto.setCity(vehicle.getCity());
            dto.setNeighbourhood(vehicle.getNeighbourhood());
            if (vehicle.getImageUrlsJson() != null) {
                try {
                    dto.setImageUrls(objectMapper.readValue(
                            vehicle.getImageUrlsJson(), new TypeReference<List<String>>() {}));
                } catch (JsonProcessingException e) {
                    log.warn("Could not deserialize imageUrlsJson for {}: {}", vehicleSourceId, e.getMessage());
                }
            }
            if (vehicle.getOrganization() != null) {
                dto.setOrganizationId(String.valueOf(vehicle.getOrganization().getId()));
            }
        } else if (ad != null) {
            // Fallback if there's no Vehicle row (shouldn't normally happen)
            dto.setTitle(ad.getTitle());
            dto.setAdSubcategory(ad.getAdSubcategory());
            dto.setDefaultImgPath(ad.getDefaultImgPath());
            dto.setCountry(ad.getCountry());
            dto.setState(ad.getState());
            dto.setCity(ad.getCity());
            dto.setNeighbourhood(ad.getNeighbourhood());
            if (ad.getOrganization() != null) {
                dto.setOrganizationId(String.valueOf(ad.getOrganization().getId()));
            }
        }

        dto.setUpdatedAt(LocalDateTime.now());
        return dto;
    }
}
