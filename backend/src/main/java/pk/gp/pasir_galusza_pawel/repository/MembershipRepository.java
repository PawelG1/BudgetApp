package pk.gp.pasir_galusza_pawel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pk.gp.pasir_galusza_pawel.model.Membership;

import java.util.List;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, Long> {
    List<Membership> findByGroupId(Long group_id);
    boolean existsByGroupIdAndUserId(Long group_id, Long userId);
    void deleteByGroupId(Long groupId);
}
