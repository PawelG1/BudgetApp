package pk.gp.pasir_galusza_pawel.exception;

import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.jspecify.annotations.NonNull;
import org.springframework.graphql.execution.DataFetcherExceptionResolver;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class GraphQLExceptionHandler implements DataFetcherExceptionResolver {

    @Override
    public Mono<List<GraphQLError>> resolveException(@NonNull Throwable ex,
                                                     @NonNull DataFetchingEnvironment env) {
        if (ex instanceof ConstraintViolationException validationEx) {
            List<GraphQLError> errors = validationEx.getConstraintViolations().stream()
                    .map(violation -> GraphqlErrorBuilder.newError(env)
                            .errorType(ErrorType.ValidationError)
                            .message("Błąd walidacji: " + violation.getPropertyPath()
                                    + " – " + violation.getMessage())
                            .build())
                    .collect(Collectors.toList());
            return Mono.just(errors);
        }

        if (ex instanceof EntityNotFoundException) {
            return single(env, ErrorType.DataFetchingException,
                    "Nie znaleziono zasobu: " + ex.getMessage());
        }

        if (ex instanceof AccessDeniedException) {
            return single(env, ErrorType.DataFetchingException,
                    "Brak uprawnień: " + ex.getMessage());
        }

        if (ex instanceof IllegalArgumentException) {
            return single(env, ErrorType.ValidationError,
                    "Nieprawidłowe dane: " + ex.getMessage());
        }

        if (ex instanceof IllegalStateException) {
            return single(env, ErrorType.ValidationError,
                    "Niedozwolona operacja: " + ex.getMessage());
        }

        // Fallback dla nieoczekiwanych wyjątków
        return single(env, ErrorType.DataFetchingException,
                "Wystąpił błąd: " + ex.getMessage());
    }

    private Mono<List<GraphQLError>> single(DataFetchingEnvironment env,
                                             ErrorType type, String message) {
        return Mono.just(List.of(
                GraphqlErrorBuilder.newError(env)
                        .errorType(type)
                        .message(message)
                        .build()
        ));
    }
}
