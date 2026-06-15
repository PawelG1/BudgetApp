package pk.gp.pasir_galusza_pawel.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import pk.gp.pasir_galusza_pawel.dto.GroupTransactionDTO;
import pk.gp.pasir_galusza_pawel.model.User;
import pk.gp.pasir_galusza_pawel.service.CurrentUserService;
import pk.gp.pasir_galusza_pawel.service.GroupTransactionService;

@Controller
public class GroupTransactionGraphQLController {

    public final GroupTransactionService groupTransactionService;
    public final CurrentUserService currentUserService;

    public GroupTransactionGraphQLController(GroupTransactionService groupTransactionService, CurrentUserService currentUserService) {
        this.groupTransactionService = groupTransactionService;
        this.currentUserService = currentUserService;
    }

    @MutationMapping
    public boolean addGroupTransaction(@Valid @Argument GroupTransactionDTO groupTransactionDTO){
        User user = currentUserService.getCurrentUser();
        groupTransactionService.addGroupTransaction(groupTransactionDTO, user);
        return true;
    }
}
