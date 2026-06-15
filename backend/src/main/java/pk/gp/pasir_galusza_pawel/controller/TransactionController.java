package pk.gp.pasir_galusza_pawel.controller;

import jakarta.validation.Valid;
import pk.gp.pasir_galusza_pawel.dto.TransactionDTO;
import pk.gp.pasir_galusza_pawel.dto.BalanceDto;
import pk.gp.pasir_galusza_pawel.model.Transaction;
import pk.gp.pasir_galusza_pawel.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pk.gp.pasir_galusza_pawel.service.TransactionService;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<List<Transaction>> GetAllTransactions(@RequestParam(required = false) String user) {
        if (user != null) {
            return ResponseEntity.ok(transactionService.getTransactionsByUser(user));
        }
        return ResponseEntity.ok(transactionService.GetAllTransactions());
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceDto> getBalance(@RequestParam(required = false) Double days) {
        return ResponseEntity.ok(transactionService.calculateBalance(days));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> GetTransactionsById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.GetTransactionById(id));
    }


    @PutMapping("/{id}")
    public ResponseEntity<Transaction> UpdateTransaction(@PathVariable Long id,
                                                         @Valid @RequestBody TransactionDTO transactionDTO) {
       return ResponseEntity.ok(transactionService.updateTransaction(id, transactionDTO));
    }

    @PostMapping
    public ResponseEntity<Transaction> CreateTransaction(@Valid @RequestBody TransactionDTO transactionDTO) {
        return ResponseEntity.ok(transactionService.createTransaction(transactionDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> DeleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
