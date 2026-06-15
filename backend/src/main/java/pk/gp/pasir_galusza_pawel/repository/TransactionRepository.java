package pk.gp.pasir_galusza_pawel.repository;

import pk.gp.pasir_galusza_pawel.model.Transaction;
import pk.gp.pasir_galusza_pawel.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findAllByUser(User user);
    List<Transaction> findByUser(User user);
    List<Transaction> findByUserEmail(String email);
    List<Transaction> findAllByUserAndTimestampGreaterThanEqual(User user, java.time.LocalDateTime timestamp);
}
