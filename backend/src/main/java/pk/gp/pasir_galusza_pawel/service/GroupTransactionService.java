package pk.gp.pasir_galusza_pawel.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import pk.gp.pasir_galusza_pawel.dto.GroupExpenseNotification;
import pk.gp.pasir_galusza_pawel.dto.GroupTransactionDTO;
import pk.gp.pasir_galusza_pawel.model.Debt;
import pk.gp.pasir_galusza_pawel.model.Group;
import pk.gp.pasir_galusza_pawel.model.Membership;
import pk.gp.pasir_galusza_pawel.model.Transaction;
import pk.gp.pasir_galusza_pawel.model.TransactionType;
import pk.gp.pasir_galusza_pawel.model.User;
import pk.gp.pasir_galusza_pawel.repository.DebtRepository;
import pk.gp.pasir_galusza_pawel.repository.GroupRepository;
import pk.gp.pasir_galusza_pawel.repository.MembershipRepository;
import pk.gp.pasir_galusza_pawel.repository.TransactionRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class GroupTransactionService {

    private final GroupRepository groupRepository;
    private final DebtRepository debtRepository;
    private final MembershipRepository membershipRepository;
    private final MembershipService membershipService;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    public GroupTransactionService(
            GroupRepository groupRepository,
            MembershipRepository membershipRepository,
            DebtRepository debtRepository,
            MembershipService membershipService,
            TransactionRepository transactionRepository,
            NotificationService notificationService
    ) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.debtRepository = debtRepository;
        this.membershipService = membershipService;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
    }

    public void addGroupTransaction(GroupTransactionDTO transactionDTO, User currentUser) {
        Group group = groupRepository.findById(transactionDTO.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono Grupy"));

        membershipService.assertCurrentUserIsGroupMember(group.getId());

        List<Membership> members = membershipRepository.findByGroupId(group.getId());
        List<Membership> selectedMembers = selectParticipants(transactionDTO, members, currentUser);
        if (selectedMembers.isEmpty()) {
            throw new IllegalStateException("Grupa nie ma czlonkow, nie mozna dodac transakcji.");
        }

        double totalAmount = transactionDTO.getAmount();
        double amountPerUser = totalAmount / selectedMembers.size();
        boolean isExpense = "EXPENSE".equals(transactionDTO.getType());

        // Dla wydatku grupowego: tworzenie osobistej transakcji EXPENSE na pełną kwotę
        // dla użytkownika dodającego (on zapłacił za wszystkich)
        if (isExpense) {
            Transaction personalExpense = new Transaction(
                    totalAmount,
                    TransactionType.EXPENSE,
                    "group:" + group.getName(),
                    transactionDTO.getTitle(),
                    currentUser
            );
            transactionRepository.save(personalExpense);
        }

        for (Membership member : selectedMembers) {
            User otherUser = member.getUser();
            if (!otherUser.getId().equals(currentUser.getId())) {
                Debt debt = new Debt();
                debt.setDebtor(isExpense ? otherUser : currentUser);
                debt.setCreditor(isExpense ? currentUser : otherUser);
                debt.setGroup(group);
                debt.setAmount(amountPerUser);
                debt.setTitle(transactionDTO.getTitle());
                debtRepository.save(debt);

                // Wysyłanie powiadomienia WebSocket do każdego uczestnika
                if (isExpense) {
                    GroupExpenseNotification notification = new GroupExpenseNotification(
                            group.getId(),
                            group.getName(),
                            transactionDTO.getTitle(),
                            totalAmount,
                            amountPerUser,
                            currentUser.getEmail()
                    );
                    notificationService.sendGroupExpenseNotification(otherUser.getEmail(), notification);
                }
            }
        }
    }

    private List<Membership> selectParticipants(
            GroupTransactionDTO transactionDTO,
            List<Membership> members,
            User currentUser) {
        List<Long> selectedUserIds = transactionDTO.getSelectedUserIds();
        if (selectedUserIds == null || selectedUserIds.isEmpty()) {
            return members;
        }
        Set<Long> uniqueSelectedUserIds = new HashSet<>(selectedUserIds);
        List<Membership> selectedMembers = members.stream()
                .filter(membership -> uniqueSelectedUserIds.contains(membership.getUser().getId()))
                .toList();
        if (selectedMembers.size() != uniqueSelectedUserIds.size()) {
            throw new IllegalStateException(
                    "Wszyscy wybrani uzytkownicy musza byc czlonkami grupy.");
        }
        boolean currentUserSelected = selectedMembers.stream()
                .anyMatch(membership -> membership.getUser().getId().equals(currentUser.getId()));
        if (!currentUserSelected) {
            throw new IllegalStateException(
                    "Aktualny uzytkownik musi byc uczestnikiem transakcji grupowej.");
        }
        if (selectedMembers.size() < 2) {
            throw new IllegalStateException("Transakcja grupowa wymaga co najmniej dwoch uczestnikow.");
        }
        return selectedMembers;
    }
}
