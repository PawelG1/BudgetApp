package pk.gp.pasir_galusza_pawel.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import pk.gp.pasir_galusza_pawel.dto.TransactionDTO;
import pk.gp.pasir_galusza_pawel.dto.BalanceDto;
import pk.gp.pasir_galusza_pawel.model.Transaction;
import pk.gp.pasir_galusza_pawel.model.TransactionType;
import pk.gp.pasir_galusza_pawel.model.User;
import pk.gp.pasir_galusza_pawel.repository.TransactionRepository;
import pk.gp.pasir_galusza_pawel.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public TransactionService(TransactionRepository transactionRepository, UserRepository userRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new AccessDeniedException("Użytkownik nie jest zalogowany");
        }

        Object principal = auth.getPrincipal();
        String email;
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Użytkownik nie istnieje"));
    }

    public List<Transaction> GetAllTransactions() {
        return transactionRepository.findByUserEmail(getAuthenticatedUser().getEmail());
    }

    public List<Transaction> getTransactionsByUser(String email) {
        User currentUser = getAuthenticatedUser();
        if (!currentUser.getEmail().equals(email)) {
            throw new AccessDeniedException("Brak uprawnień do przeglądania transakcji innego użytkownika");
        }
        return transactionRepository.findByUserEmail(email);
    }

    public BalanceDto calculateBalance(Double days) {
        User user = getAuthenticatedUser();
        List<Transaction> transactions;
        
        if (days != null && days > 0) {
            long secondsToSubtract = (long) (days * 24 * 60 * 60);
            LocalDateTime startDate = LocalDateTime.now().minusSeconds(secondsToSubtract);
            transactions = transactionRepository.findAllByUserAndTimestampGreaterThanEqual(user, startDate);
        } else {
            transactions = transactionRepository.findByUser(user);
        }
        
        double totalIncome = transactions.stream()
                .filter(t -> t.getType() == TransactionType.INCOME)
                .mapToDouble(Transaction::getAmount)
                .sum();
                
        double totalExpense = transactions.stream()
                .filter(t -> t.getType() == TransactionType.EXPENSE)
                .mapToDouble(Transaction::getAmount)
                .sum();
                
        double balance = totalIncome - totalExpense;
        return new BalanceDto(totalIncome, totalExpense, balance);
    }

    public Transaction GetTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Nie znaleziono transakcji o ID " + id));
        if (!transaction.getUser().getEmail().equals(getAuthenticatedUser().getEmail())) {
            throw new AccessDeniedException("Brak uprawnień do tej transakcji");
        }
        return transaction;
    }

    public Transaction createTransaction(TransactionDTO transactionDTO) {
        Transaction transaction = new Transaction();
        transaction.setAmount(transactionDTO.getAmount());
        transaction.setType(TransactionType.valueOf(transactionDTO.getType()));
        transaction.setTags(transactionDTO.getTags());
        transaction.setNotes(transactionDTO.getNotes());
        transaction.setUser(getAuthenticatedUser());
        
        if (transactionDTO.getTimestamp() != null && !transactionDTO.getTimestamp().isEmpty()) {
            try {
                // Remove trailing "Z" if present (often sent by frontends) to easily parse locally
                String ts = transactionDTO.getTimestamp().replace("Z", "");
                transaction.setTimestamp(LocalDateTime.parse(ts));
            } catch (Exception e) {
                transaction.setTimestamp(LocalDateTime.now());
            }
        } else {
            transaction.setTimestamp(LocalDateTime.now());
        }
        
        return transactionRepository.save(transaction);
    }

    public Transaction updateTransaction(Long id, TransactionDTO transactionDTO) {
        Transaction transaction = GetTransactionById(id);
        transaction.setAmount(transactionDTO.getAmount());
        transaction.setType(TransactionType.valueOf(transactionDTO.getType()));
        transaction.setTags(transactionDTO.getTags());
        transaction.setNotes(transactionDTO.getNotes());
        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id) {
        Transaction transaction = GetTransactionById(id);
        transactionRepository.deleteById(id);
    }
}
