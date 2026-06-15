package pk.gp.pasir_galusza_pawel.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import pk.gp.pasir_galusza_pawel.dto.MembershipDTO;
import pk.gp.pasir_galusza_pawel.model.Group;
import pk.gp.pasir_galusza_pawel.model.Membership;
import pk.gp.pasir_galusza_pawel.model.User;
import pk.gp.pasir_galusza_pawel.repository.GroupRepository;
import pk.gp.pasir_galusza_pawel.repository.MembershipRepository;
import pk.gp.pasir_galusza_pawel.repository.UserRepository;

import java.util.List;

@Service
public class MembershipService {
    private final MembershipRepository membershipRepository;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public MembershipService(MembershipRepository membershipRepository, GroupRepository groupRepository, UserRepository userRepository) {
        this.membershipRepository = membershipRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.currentUserService = new CurrentUserService(userRepository);
    }

    public List<Membership> getGroupMembers(Long groupId) {
        assertCurrentUserIsGroupMember(groupId);
        return membershipRepository.findByGroupId(groupId);
    }

    public Membership addMember(MembershipDTO  membershipDTO) {
        assertCurrentUserIsGroupOwner(membershipDTO.getGroupId());

        User user = userRepository.findByEmail(membershipDTO.getUserEmail())
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono uzytkownika o emailu " + membershipDTO.getUserEmail()));
        Group group = groupRepository.findById(membershipDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono grupy o id " + membershipDTO.getGroupId()));

        boolean alreadyMember = membershipRepository.findByGroupId(group.getId()).stream()
                .anyMatch(membership -> membership.getUser().getId().equals(user.getId()));

        if(alreadyMember) {
            throw new IllegalStateException("Uzytkownik jest juz czlonkiem tej grupy.");
        }

        Membership membership = new Membership();
        membership.setUser(user);
        membership.setGroup(group);

        return membershipRepository.save(membership);
    }

    public void removeMember(Long membershipId) {
        Membership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono czlonkostwa o id " + membershipId));

        User currentUser = currentUserService.getCurrentUser();
        User groupOwner = membership.getGroup().getOwner();

        if(!currentUser.getId().equals(groupOwner.getId())){
            throw new AccessDeniedException("Tylko wlasciciel grupy moze usuwac czlonkow");
        }

        if(membership.getUser().getId().equals(groupOwner.getId())){
            throw new IllegalStateException("Nie mozna usunac wlasciciela grupy z grupy");
        }

        membershipRepository.delete(membership);
    }

    public void assertCurrentUserIsGroupMember(Long groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(()-> new EntityNotFoundException("Nie znaleziono grupy o id " + groupId));

        User currentUser = currentUserService.getCurrentUser();
        assertUserIsGroupMember(groupId, currentUser.getId());

    }

    public void assertCurrentUserIsGroupOwner(Long groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(()-> new EntityNotFoundException("Nie znaleziono grupy o id " + groupId));

        User currentUser = currentUserService.getCurrentUser();
        if(!group.getOwner().getId().equals(currentUser.getId())){
            throw new AccessDeniedException("Tylko wlasciciel grupy moze wykonac ta operacje");
        }
    }

    public void assertUserIsGroupMember(Long groupId, Long userId){
        if(!membershipRepository.existsByGroupIdAndUserId(groupId, userId)){
            throw new AccessDeniedException("Uzytkownik o id " + userId + "nie jest czlonkiem grupy o id " + groupId);
        }
    }
}
