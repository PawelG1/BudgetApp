package pk.gp.pasir_galusza_pawel.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import pk.gp.pasir_galusza_pawel.dto.DebtDTO;
import pk.gp.pasir_galusza_pawel.dto.DebtNotification;
import pk.gp.pasir_galusza_pawel.model.Debt;
import pk.gp.pasir_galusza_pawel.model.Group;
import pk.gp.pasir_galusza_pawel.model.Transaction;
import pk.gp.pasir_galusza_pawel.model.TransactionType;
import pk.gp.pasir_galusza_pawel.model.User;
import pk.gp.pasir_galusza_pawel.repository.DebtRepository;
import pk.gp.pasir_galusza_pawel.repository.GroupRepository;
import pk.gp.pasir_galusza_pawel.repository.MembershipRepository;
import pk.gp.pasir_galusza_pawel.repository.TransactionRepository;
import pk.gp.pasir_galusza_pawel.repository.UserRepository;

import java.util.List;

@Service
public class DebtService {
    private final DebtRepository debtRepository;
    private final MembershipService membershipService;
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final TransactionRepository transactionRepository;
    private final NotificationService notificationService;

    public DebtService(
            DebtRepository debtRepository,
            GroupRepository groupRepository,
            UserRepository userRepository,
            MembershipService membershipService,
            CurrentUserService currentUserService,
            TransactionRepository transactionRepository,
            NotificationService notificationService
    ) {
        this.debtRepository = debtRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.membershipService = membershipService;
        this.currentUserService = currentUserService;
        this.transactionRepository = transactionRepository;
        this.notificationService = notificationService;
    }

    public List<Debt> getGroupDebts(Long groupId){
        membershipService.assertCurrentUserIsGroupMember(groupId);
        return debtRepository.findByGroupId(groupId);
    }

    public Debt createDebt(DebtDTO debtDTO){
        Group group = groupRepository.findById(debtDTO.getGroupId()).orElseThrow(()-> new EntityNotFoundException(
                "nie mozna utworzyc dlufu grupa:" + debtDTO.getGroupId() + "nie istnieje"
        ));

        User debtor = userRepository.findById(debtDTO.getDebtorId()).orElseThrow(()-> new EntityNotFoundException(
                "Nie mozna utworzyc dlugu: dluznik o id " + debtDTO.getDebtorId() + "nie istnieje"
        ));

        User creditor = userRepository.findById(debtDTO.getCreditorId()).orElseThrow(()-> new EntityNotFoundException(
                "Nie mozna utworzyc dlugu: wierzyciel o id " + debtDTO.getCreditorId() + "nie istnieje"
        ));

        membershipService.assertCurrentUserIsGroupMember(group.getId());
        membershipService.assertUserIsGroupMember(group.getId(), debtor.getId());
        membershipService.assertUserIsGroupMember(group.getId(), creditor.getId());

        if(debtor.getId().equals(creditor.getId())){
            throw new IllegalArgumentException("Nie można utworzyć długu: dłużnik i wierzyciel nie mogą być tym samym użytkownikiem");
        }

        User currentUser = currentUserService.getCurrentUser();
        assertCurrentUserCanManageDebt(group, debtor, creditor, currentUser);

        Debt debt = new Debt();
        debt.setGroup(group);
        debt.setDebtor(debtor);
        debt.setCreditor(creditor);
        debt.setAmount(debtDTO.getAmount());
        debt.setTitle(debtDTO.getTitle());

        return  debtRepository.save(debt);
    }

    public void deleteDebt(Long debtId){
        Debt debt = debtRepository.findById(debtId).orElseThrow(()-> new EntityNotFoundException(
                "Nie mozna usunac dlugu, dlug o id " + debtId + "nie istnieje"
        ));

        membershipService.assertCurrentUserIsGroupMember(debt.getGroup().getId());
        User currentUser = currentUserService.getCurrentUser();
        assertCurrentUserCanManageDebt(debt.getGroup(), debt.getDebtor(), debt.getCreditor(), currentUser);
        debtRepository.delete(debt);
    }

    public Debt markDebtAsPaid(Long debtId){
        Debt debt = debtRepository.findById(debtId).orElseThrow(()-> new EntityNotFoundException(
                "Nie mozna oznaczyc dlugu jako oplacony, dlug o id " + debtId + "nie istnieje"
        ));
        User currentUser = currentUserService.getCurrentUser();

        if(!debt.getDebtor().getId().equals(currentUser.getId())){
            throw new AccessDeniedException("Tylko dluznik moze oznaczyc dlug jako oplacony");
        }

        debt.setPaidByDebtor(true);
        debt.setConfirmedByCreditor(false);
        Debt saved = debtRepository.save(debt);

        // Aktualizacja bilansu dłużnika: spłacił swoją część (EXPENSE)
        Transaction debtorExpense = new Transaction(
                debt.getAmount(),
                TransactionType.EXPENSE,
                "group-debt:" + debt.getGroup().getName(),
                "Spłata: " + debt.getTitle(),
                currentUser
        );
        transactionRepository.save(debtorExpense);

        // Powiadomienie WebSocket do wierzyciela
        String msg = String.format("%s oznaczył spłatę długu \"%s\" w grupie %s. Oczekuje na Twoje potwierdzenie.",
                currentUser.getEmail(), debt.getTitle(), debt.getGroup().getName());
        notificationService.sendDebtNotification(
                debt.getCreditor().getEmail(),
                new DebtNotification("DEBT_MARKED_AS_PAID", debt.getGroup().getId(),
                        debt.getGroup().getName(), debt.getTitle(), debt.getAmount(), msg)
        );

        return saved;
    }

    public Debt confirmDebtPayment(Long debtId) {
        Debt debt = getDebtForCurrentGroupMember(debtId);
        User currentUser = currentUserService.getCurrentUser();
        if (!debt.getCreditor().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Tylko wierzyciel moze potwierdzic splate dlugu.");
        }
        if (!debt.isPaidByDebtor()) {
            throw new IllegalStateException(
                    "Dlug musi zostac najpierw oznaczony jako oplacony przez dluznika.");
        }
        debt.setConfirmedByCreditor(true);
        Debt saved = debtRepository.save(debt);

        // Aktualizacja bilansu: wierzyciel dostaje z powrotem swoją część (INCOME)
        Transaction repaymentIncome = new Transaction(
                debt.getAmount(),
                TransactionType.INCOME,
                "group-repayment:" + debt.getGroup().getName(),
                "Spłata: " + debt.getTitle(),
                currentUser
        );
        transactionRepository.save(repaymentIncome);

        // Powiadomienie WebSocket do dłużnika
        String msg = String.format("%s potwierdził spłatę długu \"%s\" w grupie %s. Dług został rozliczony.",
                currentUser.getEmail(), debt.getTitle(), debt.getGroup().getName());
        notificationService.sendDebtNotification(
                debt.getDebtor().getEmail(),
                new DebtNotification("DEBT_PAYMENT_CONFIRMED", debt.getGroup().getId(),
                        debt.getGroup().getName(), debt.getTitle(), debt.getAmount(), msg)
        );

        return saved;
    }

    private Debt getDebtForCurrentGroupMember(Long debtId) {
        Debt debt = debtRepository.findById(debtId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Nie znaleziono dlugu o ID " + debtId + "."));
        membershipService.assertCurrentUserIsGroupMember(debt.getGroup().getId());
        return debt;
    }

    private void assertCurrentUserCanManageDebt(Group group, User debtor, User creditor, User currentUser){
        boolean isGroupOwner = group.getOwner().getId().equals(currentUser.getId());
        boolean isDebtParticipant = debtor.getId().equals(currentUser.getId())
                || creditor.getId().equals(currentUser.getId());

        if(!isGroupOwner && !isDebtParticipant){
            throw new AccessDeniedException(
                    "Tylko wlasciciel groupy albo uczestnik dlugu moze wykoac te operacje"
            );
        }
    }
}
