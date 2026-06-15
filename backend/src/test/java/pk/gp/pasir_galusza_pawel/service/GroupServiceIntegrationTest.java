package pk.gp.pasir_galusza_pawel.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import pk.gp.pasir_galusza_pawel.dto.GroupDTO;
import pk.gp.pasir_galusza_pawel.model.Group;
import pk.gp.pasir_galusza_pawel.model.User;
import pk.gp.pasir_galusza_pawel.repository.GroupRepository;
import pk.gp.pasir_galusza_pawel.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class GroupServiceIntegrationTest {

    @Autowired
    private GroupService groupService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @BeforeEach
    void setUp() {
        if (userRepository.findByEmail("test@owner.com").isEmpty()) {
            User user = new User();
            user.setEmail("test@owner.com");
            user.setUsername("testOwner");
            user.setPassword("password123");
            userRepository.save(user);
        }
    }

    @Test
    @WithMockUser(username = "test@owner.com")
    void createGroup_shouldAddOwnerAsMemberAndReturnItInGetAllGroups() {
        // Arrange
        GroupDTO groupDTO = new GroupDTO();
        groupDTO.setName("Test Group");

        // Act
        Group createdGroup = groupService.createGroup(groupDTO);
        List<Group> myGroups = groupService.getAllGroups();

        // Assert
        assertNotNull(createdGroup.getId(), "Stworzona grupa powinna miec przypisane ID");
        assertEquals("Test Group", createdGroup.getName(), "Nazwa grupy powinna sie zgadzac");
        assertEquals("test@owner.com", createdGroup.getOwner().getEmail(), "Wlasciciel powinien miec poprawny email");

        assertTrue(myGroups.stream().anyMatch(g -> g.getId().equals(createdGroup.getId())),
                "Zwrócona lista myGroups powinna zawierac nowo utworzoną grupę, więc wlasciciel jest też jej czlonkiem");
    }
}

