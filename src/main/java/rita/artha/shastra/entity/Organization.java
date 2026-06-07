package rita.artha.shastra.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "organization")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Organization {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String logoPath;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_person_id", nullable = false)
    private Person owner;

    @Column(nullable = false)
    private String subscriptionTier = "FREE";

    @Column(nullable = false)
    private String status = "ACTIVE";

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY)
    private java.util.List<OrganizationMember> members;
}
