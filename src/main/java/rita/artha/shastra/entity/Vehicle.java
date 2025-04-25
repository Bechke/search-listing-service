package rita.artha.shastra.entity;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

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
    private String country;
    private String state;
    private String city;
    private String neighbourhood;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
