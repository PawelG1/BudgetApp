package pk.gp.pasir_galusza_pawel.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import pk.gp.pasir_galusza_pawel.model.Debt;
import pk.gp.pasir_galusza_pawel.model.Group;
import pk.gp.pasir_galusza_pawel.model.Membership;
import pk.gp.pasir_galusza_pawel.model.User;
import pk.gp.pasir_galusza_pawel.repository.DebtRepository;
import pk.gp.pasir_galusza_pawel.repository.GroupRepository;
import pk.gp.pasir_galusza_pawel.repository.MembershipRepository;
import pk.gp.pasir_galusza_pawel.repository.UserRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class DebtServiceIntegrationTest {

    @Autowired
    private DebtService debtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private MembershipRepository membershipRepository;

    @Autowired
    private DebtRepository debtRepository;

    @BeforeEach
    void setUp() {
        if (userRepository.findByEmail("test@owner.com").isEmpty()) {
            User owner = new User();
            owner.setEmail("test@owner.com");
            owner.setUsername("testOwner");
            owner.setPassword("password123");
            userRepository.save(owner);
        }
        if (userRepository.findByEmail("invited@test.com").isEmpty()) {
            User invited = new User();
            invited.setEmail("invited@test.com");
            invited.setUsername("invitedUser");
            invited.setPassword("password123");
            userRepository.save(invited);
        }
        if (userRepository.findByEmail("notmember@test.com").isEmpty()) {
            User notMember = new User();
            notMember.setEmail("notmember@test.com");
            notMember.setUsername("notMemberUser");
            notMember.setPassword("password123");
            userRepository.save(notMember);
        }
    }

    @Test
    @WithMockUser(username = "invited@test.com")
    void getGroupDebts_asMember_shouldReturnDebts() {
        User owner = userRepository.findByEmail("test@owner.com").orElseThrow();
        User member = userRepository.findByEmail("invited@test.com").orElseThrow();

        Group group = new Group();
        group.setName("Debt Group");
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

        Debt debt = new Debt();
        debt.setGroup(group);
        debt.setCreditor(owner);
        debt.setDebtor(member);
        debt.setAmount(100.0);
        debt.setTitle("Lunch");
        debtRepository.save(debt);

        List<Debt> debts = debtService.getGroupDebts(group.getId());

        assertEquals(1, debts.size(), "Powinno zwrócić jeden dług");
        assertEquals("Lunch", debts.get(0).getTitle());
    }

    @Test
    @WithMockUser(username = "notmember@test.com")
    void getGroupDebts_asNonMember_shouldThrowAccessDeniedException() {
        User owner = userRepository.findByEmail("test@owner.com").orElseThrow();

        Group group = new Group();
        group.setName("Secret Debt Group");
        group.setOwner(owner);
        group = groupRepository.save(group);

        Membership m1 = new Membership();
        m1.setGroup(group);
        m1.setUser(owner);
        membershipRepository.save(m1);

        Debt debt = new Debt();
        debt.setGroup(group);
        debt.setCreditor(owner);
        debt.setDebtor(owner);
        debt.setAmount(50.0);
        debt.setTitle("Test");
        debtRepository.save(debt);

        Long groupId = group.getId();
        assertThrows(AccessDeniedException.class, () -> debtService.getGroupDebts(groupId));
    }
}

