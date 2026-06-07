package rita.artha.shastra.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer vehicleId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /** MongoDB _id from vehicle-service — used to upsert on UPDATE/DELETE events */
    @Column(unique = true)
    private String vehicleSourceId;

    @JsonIgnore
    private String mobile;
    private String adSubcategory;
    private String brand;
    private Integer year;
    private String fuelType;
    private String transmission;
    private Integer odometerReading;
    private Integer numOwners;
    private String title;
    private String description;
    private Double price;
    private String defaultImgPath;

    @Column(columnDefinition = "TEXT")
    private String imageUrlsJson; // stored as JSON string, e.g. ["vehicles/a.jpg","vehicles/b.jpg"]
    private String country;
    private String state;
    private String city;
    private String neighbourhood;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;
}
