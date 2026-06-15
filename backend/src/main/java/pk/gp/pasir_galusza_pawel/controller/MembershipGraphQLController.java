package pk.gp.pasir_galusza_pawel.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import pk.gp.pasir_galusza_pawel.dto.GroupResponseDTO;
import pk.gp.pasir_galusza_pawel.dto.MembershipDTO;
import pk.gp.pasir_galusza_pawel.dto.MembershipResponseDTO;
import pk.gp.pasir_galusza_pawel.model.Membership;
import pk.gp.pasir_galusza_pawel.model.User;
import pk.gp.pasir_galusza_pawel.repository.GroupRepository;
import pk.gp.pasir_galusza_pawel.repository.MembershipRepository;
import pk.gp.pasir_galusza_pawel.service.CurrentUserService;
import pk.gp.pasir_galusza_pawel.service.MembershipService;

import java.util.List;

@Controller
public class MembershipGraphQLController {
    private final MembershipService membershipService;
    private final GroupRepository groupRepository;
    private final CurrentUserService currentUserService;

    public MembershipGraphQLController(MembershipService membershipService, GroupRepository groupRepository, CurrentUserService currentUserService) {
        this.membershipService = membershipService;
        this.groupRepository = groupRepository;
        this.currentUserService = currentUserService;
    }

    @QueryMapping
    public List<MembershipResponseDTO> groupMembers(@Argument Long groupId){
        return membershipService.getGroupMembers(groupId).stream()
                .map(membership -> new MembershipResponseDTO(
                        membership.getId(),
                        membership.getUser().getId(),
                        membership.getGroup().getId(),
                        membership.getUser().getEmail()
                )).toList();
    }

    @MutationMapping
    public MembershipResponseDTO addMember(@Valid @Argument MembershipDTO membershipDTO){
        Membership membership = membershipService.addMember(membershipDTO);
        return new MembershipResponseDTO(
                membership.getId(),
                membership.getUser().getId(),
                membership.getGroup().getId(),
                membership.getUser().getEmail()
        );
    }

    @QueryMapping
    public List<GroupResponseDTO> myGroups(){
        User currentUser = currentUserService.getCurrentUser();
        return groupRepository.findByMemberships_User(currentUser).stream()
                .map(group -> new GroupResponseDTO(
                        group.getId(),
                        group.getName(),
                        group.getOwner().getId()
                )).toList();
    }

    @MutationMapping
    public boolean removeMember(@Argument Long membershipId){
        membershipService.removeMember(membershipId);
        return true;
    }
}
