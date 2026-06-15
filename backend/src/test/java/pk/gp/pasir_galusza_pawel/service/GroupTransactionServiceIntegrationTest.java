package pk.gp.pasir_galusza_pawel.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;
import pk.gp.pasir_galusza_pawel.dto.GroupTransactionDTO;
import pk.gp.pasir_galusza_pawel.model.Group;
import pk.gp.pasir_galusza_pawel.model.Membership;
import pk.gp.pasir_galusza_pawel.model.User;
import pk.gp.pasir_galusza_pawel.repository.DebtRepository;
import pk.gp.pasir_galusza_pawel.repository.GroupRepository;
import pk.gp.pasir_galusza_pawel.repository.MembershipRepository;
import pk.gp.pasir_galusza_pawel.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class GroupTransactionServiceIntegrationTest {

    @Autowired
    private GroupTransactionService groupTransactionService;

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
        if (userRepository.findByEmail("borrower1@test.com").isEmpty()) {
            User b1 = new User();
            b1.setEmail("borrower1@test.com");
            b1.setUsername("borrower1");
            b1.setPassword("password123");
            userRepository.save(b1);
        }
        if (userRepository.findByEmail("borrower2@test.com").isEmpty()) {
            User b2 = new User();
            b2.setEmail("borrower2@test.com");
            b2.setUsername("borrower2");
            b2.setPassword("password123");
            userRepository.save(b2);
        }
        if (userRepository.findByEmail("creator@test.com").isEmpty()) {
            User cr = new User();
            cr.setEmail("creator@test.com");
            cr.setUsername("creatorUser");
            cr.setPassword("password123");
            userRepository.save(cr);
        }
    }

    @Test
    @WithMockUser(username = "creator@test.com")
    void addGroupTransaction_asIncome_shouldCreateDebtsFromCurrentUserToOthers() {
        // Arrange
        User creator = userRepository.findByEmail("creator@test.com").orElseThrow();
        User borrower1 = userRepository.findByEmail("borrower1@test.com").orElseThrow();
        User borrower2 = userRepository.findByEmail("borrower2@test.com").orElseThrow();

        Group group = new Group();
        group.setName("Shared Expenses");
        group.setOwner(creator);
        group = groupRepository.save(group);

        Membership m1 = new Membership();
        m1.setGroup(group);
        m1.setUser(creator);
        membershipRepository.save(m1);

        Membership m2 = new Membership();
        m2.setGroup(group);
        m2.setUser(borrower1);
        membershipRepository.save(m2);

        Membership m3 = new Membership();
        m3.setGroup(group);
        m3.setUser(borrower2);
        membershipRepository.save(m3);

        GroupTransactionDTO dto = new GroupTransactionDTO();
        dto.setGroupId(group.getId());
        dto.setAmount(300.0);
        dto.setType("INCOME");
        dto.setTitle("Wplata - zaliczka od innych");

        // Act
        groupTransactionService.addGroupTransaction(dto, creator);

        // Assert
        var debts = debtRepository.findByGroupId(group.getId());
        assertEquals(2, debts.size(), "Powinno wygenerować 2 długi dal 2 pozostałych członków");

        // Each debt should be 100 (300 / 3 members = 100)
        assertTrue(debts.stream().allMatch(d -> d.getAmount() == 100.0));

        // Since it's INCOME, exact rule is: creditor = otherUser, debtor = creator
        // Wymaganie: tworzy długi od aktualnego użytkownika (debtor) do pozostałych członków (creditors)
        assertTrue(debts.stream().allMatch(d -> d.getDebtor().getId().equals(creator.getId())), "Aktualny użytkownik powinnien być dłużnikiem (debtor)");
        assertTrue(debts.stream().anyMatch(d -> d.getCreditor().getId().equals(borrower1.getId())), "Zapożyczony 1 powinien być wierzycielem");
        assertTrue(debts.stream().anyMatch(d -> d.getCreditor().getId().equals(borrower2.getId())), "Zapożyczony 2 powinien być wierzycielem");
    }
}

