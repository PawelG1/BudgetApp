package pk.gp.pasir_galusza_pawel.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import pk.gp.pasir_galusza_pawel.dto.GroupDTO;
import pk.gp.pasir_galusza_pawel.dto.MembershipDTO;
import pk.gp.pasir_galusza_pawel.model.Group;
import pk.gp.pasir_galusza_pawel.model.Membership;
import pk.gp.pasir_galusza_pawel.model.User;
import pk.gp.pasir_galusza_pawel.repository.GroupRepository;
import pk.gp.pasir_galusza_pawel.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class MembershipServiceIntegrationTest {

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private pk.gp.pasir_galusza_pawel.repository.MembershipRepository membershipRepository;

    @BeforeEach
    void setUp() {
        if (userRepository.findByEmail("test@owner.com").isEmpty()) {
            User owner = new User();
            owner.setEmail("test@owner.com");
            owner.setUsername("testOwner");
            owner.setPassword("password123");
            userRepository.save(owner);
        }
        if (userRepository.findByEmail("notowner@test.com").isEmpty()) {
            User notOwner = new User();
            notOwner.setEmail("notowner@test.com");
            notOwner.setUsername("notOwner");
            notOwner.setPassword("password123");
            userRepository.save(notOwner);
        }
        if (userRepository.findByEmail("invited@test.com").isEmpty()) {
            User invited = new User();
            invited.setEmail("invited@test.com");
            invited.setUsername("invitedUser");
            invited.setPassword("password123");
            userRepository.save(invited);
        }
    }

    @Test
    @WithMockUser(username = "test@owner.com")
    void addMember_asOwner_shouldAddSuccessfully() {
        // Arrange
        GroupDTO groupDTO = new GroupDTO();
        groupDTO.setName("Owner's Group");
        Group createdGroup = groupService.createGroup(groupDTO);

        MembershipDTO membershipDTO = new MembershipDTO();
        membershipDTO.setGroupId(createdGroup.getId());
        membershipDTO.setUserEmail("invited@test.com");

        // Act
        Membership membership = membershipService.addMember(membershipDTO);

        // Assert
        assertNotNull(membership.getId(), "Członkostwo powinno mieć wygenerowane ID");
        assertEquals("invited@test.com", membership.getUser().getEmail(), "Email zaproszonego użytkownika powinien się zgadzać");
        assertEquals(createdGroup.getId(), membership.getGroup().getId(), "ID grupy powinno być poprawne");
    }

    @Test
    @WithMockUser(username = "notowner@test.com")
    void addMember_asNonOwner_shouldThrowAccessDeniedException() {
        // Arrange
        User owner = userRepository.findByEmail("test@owner.com").orElseThrow();
        Group group = new Group();
        group.setName("Owner's Group");
        group.setOwner(owner);
        group = groupRepository.save(group);

        MembershipDTO membershipDTO = new MembershipDTO();
        membershipDTO.setGroupId(group.getId());
        membershipDTO.setUserEmail("invited@test.com");

        // Act & Assert
        Long groupId = group.getId();
        assertThrows(AccessDeniedException.class, () -> membershipService.addMember(membershipDTO));
    }

    @Test
    @WithMockUser(username = "invited@test.com")
    void getGroupMembers_asMember_shouldReturnMembers() {
        // Arrange
        User owner = userRepository.findByEmail("test@owner.com").orElseThrow();
        User member = userRepository.findByEmail("invited@test.com").orElseThrow();

        Group group = new Group();
        group.setName("Test Group");
        group.setOwner(owner);
        group = groupRepository.save(group);

        Membership m1 = new Membership();
        m1.setGroup(group);
        m1.setUser(owner);
        membershipRepository.save(m1);

        Membership m2 = new Membership();
        m2.setGroup(group);
        m2.setUser(member);
        membershipRepository.save(m2);

        // Act
        var members = membershipService.getGroupMembers(group.getId());

        // Assert
        assertEquals(2, members.size(), "Powinno zwrócić dwóch członków");
        assertTrue(members.stream().anyMatch(m -> m.getUser().getEmail().equals("test@owner.com")));
        assertTrue(members.stream().anyMatch(m -> m.getUser().getEmail().equals("invited@test.com")));
    }

    @Test
    @WithMockUser(username = "notowner@test.com")
    void getGroupMembers_asNonMember_shouldThrowAccessDeniedException() {
        // Arrange
        User owner = userRepository.findByEmail("test@owner.com").orElseThrow();

        Group group = new Group();
        group.setName("Secret Group");
        group.setOwner(owner);
        group = groupRepository.save(group);

        // Act & Assert
        Long groupId = group.getId();
        assertThrows(AccessDeniedException.class, () -> membershipService.getGroupMembers(groupId));
    }
}
