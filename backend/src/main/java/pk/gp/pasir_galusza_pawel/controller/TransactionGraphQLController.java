package pk.gp.pasir_galusza_pawel.controller;

import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import pk.gp.pasir_galusza_pawel.dto.TransactionDTO;
import pk.gp.pasir_galusza_pawel.dto.BalanceDto;
import reactor.core.publisher.Mono;
import org.springframework.security.core.Authentication;
import pk.gp.pasir_galusza_pawel.model.Transaction;
import pk.gp.pasir_galusza_pawel.service.TransactionService;
import java.util.Collections;

import java.util.List;

@Controller
@Configuration
public class TransactionGraphQLController {
    private final TransactionService transactionService;

    public TransactionGraphQLController(TransactionService transactionService){
        this.transactionService = transactionService;
    }
    
    @Bean
    public WebGraphQlInterceptor documentInterceptor() {
        return new WebGraphQlInterceptor() {
            @Override
            public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest request, Chain chain) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                boolean isAuthenticated = auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser");
                if (isAuthenticated) {
                    request.configureExecutionInput((executionInput, builder) ->
                            builder.graphQLContext(Collections.singletonMap("email", auth.getName())).build()
                    );
                }
                return chain.next(request);
            }
        };
    }

    @QueryMapping
    public List<Transaction> transactions(@org.springframework.graphql.data.method.annotation.ContextValue(name = "email", required = false) String email){
        if (email == null) {
            throw new org.springframework.security.access.AccessDeniedException("Użytkownik nie jest zalogowany");
        }
        var authWrapper = new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authWrapper);
        try {
            return transactionService.GetAllTransactions();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @QueryMapping
    public BalanceDto balance(@org.springframework.graphql.data.method.annotation.ContextValue(name = "email", required = false) String email, @Argument Double days) {
        if (email == null) {
            throw new org.springframework.security.access.AccessDeniedException("Użytkownik nie jest zalogowany");
        }
        var authWrapper = new UsernamePasswordAuthenticationToken(email, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authWrapper);
        try {
            return transactionService.calculateBalance(days);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @QueryMapping
    public BalanceDto userBalance(@org.springframework.graphql.data.method.annotation.ContextValue(name = "email", required = false) String email, @Argument Double days) {
        return balance(email, days);
    }

    @MutationMapping
    public Transaction addTransaction(
        @Valid @Argument TransactionDTO transactionDTO){
        return transactionService.createTransaction(transactionDTO);
    }

    @MutationMapping
    public Transaction updateTransaction(
            @Argument Long id,
    @Valid @Argument TransactionDTO transactionDTO){
        return transactionService.updateTransaction(id, transactionDTO);
    }

    @MutationMapping
    public boolean deleteTransaction(
        @Argument Long id){
            transactionService.deleteTransaction(id);
            return true;

    }
}
