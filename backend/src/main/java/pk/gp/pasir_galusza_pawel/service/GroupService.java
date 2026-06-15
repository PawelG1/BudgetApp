package pk.gp.pasir_galusza_pawel.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import pk.gp.pasir_galusza_pawel.dto.GroupDTO;
import pk.gp.pasir_galusza_pawel.model.Group;
import pk.gp.pasir_galusza_pawel.model.Membership;
import pk.gp.pasir_galusza_pawel.model.User;
import pk.gp.pasir_galusza_pawel.repository.DebtRepository;
import pk.gp.pasir_galusza_pawel.repository.GroupRepository;
import pk.gp.pasir_galusza_pawel.repository.MembershipRepository;

import java.util.List;

@Service
public class GroupService {
    private final GroupRepository groupRepository;
    private final MembershipRepository membershipRepository;
    private final DebtRepository debtRepository;
    private final CurrentUserService currentUserService;

    public GroupService(
            GroupRepository groupRepository,
            MembershipRepository membershipRepository,
            DebtRepository debtRepository,
            CurrentUserService currentUserService
    ){
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.debtRepository = debtRepository;
        this.currentUserService = currentUserService;
    }

    public List<Group> getAllGroups() {
        User currentUser = currentUserService.getCurrentUser();
        return groupRepository.findByMemberships_User(currentUser);
    }

    public Group createGroup(GroupDTO groupDTO) {
        User owner = currentUserService.getCurrentUser();
        Group group = new Group();
        group.setName(groupDTO.getName());
        group.setOwner(owner);
        Group savedGroup = groupRepository.save(group);
        Membership membership = new Membership();
        membership.setUser(owner);
        membership.setGroup(savedGroup);
        membershipRepository.save(membership);
        return savedGroup;
    }

    @Transactional
    public void deleteGroup(Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Grupa z id:" + id + "nie istnieje"));

        User currentUser = currentUserService.getCurrentUser();
        if(!(group.getOwner().getId() == (currentUser.getId()))) {
            throw new RuntimeException("tylko wlasciciel grupy moze ja usunac");
        }

        debtRepository.deleteByGroupId(id);
        membershipRepository.deleteByGroupId(id);
        groupRepository.delete(group);
    }
}
