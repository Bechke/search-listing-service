package rita.artha.shastra.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleDTO {

    private Integer personId;
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