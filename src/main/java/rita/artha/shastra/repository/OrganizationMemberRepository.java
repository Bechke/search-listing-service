package rita.artha.shastra.repository;

import rita.artha.shastra.entity.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Integer> {
    List<OrganizationMember> findByOrganization_Id(Integer orgId);
    Optional<OrganizationMember> findByOrganization_IdAndPerson_PersonId(Integer orgId, Integer personId);
    Optional<OrganizationMember> findByOrganization_IdAndPerson_KeycloakId(Integer orgId, String keycloakId);
    List<OrganizationMember> findByPerson_KeycloakId(String keycloakId);
    boolean existsByOrganization_IdAndPerson_PersonId(Integer orgId, Integer personId);
}
