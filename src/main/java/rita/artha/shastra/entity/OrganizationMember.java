package rita.artha.shastra.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "organization_member",
       uniqueConstraints = @UniqueConstraint(columnNames = {"organization_id", "person_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrganizationMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(nullable = false)
    private String role; // OWNER | STAFF

    private LocalDateTime joinedAt;
}
