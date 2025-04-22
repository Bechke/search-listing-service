package rita.artha.shastra.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VehicleBrandDto {
    @NotBlank(message = "Brand name is required")
    private String name;

    @NotNull(message = "Category ID is required")
    private Integer categoryId;
}