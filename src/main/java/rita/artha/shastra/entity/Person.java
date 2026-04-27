package rita.artha.shastra.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer personId;

    private String fullName;

    /** Keycloak sub claim — the stable unique ID for this seller across all services */
    @Column(unique = true)
    private String keycloakId;

    @Column(unique = true)
    private String company;

    private String mobileNumber;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastActivity;
}
