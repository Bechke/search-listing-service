package rita.artha.shastra.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Advertisement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer advertisementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;

    private String adCategory;
    private String adSubcategory;
    private String country;
    private String state;
    private String city;
    private String neighbourhood;
    private String title;
    private String defaultImgPath;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
