package pk.gp.pasir_galusza_pawel.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GroupTransactionDTO {

    @NotNull(message = "Id grupy nie moze byc puste")
    private Long groupId;

    @NotNull(message = "Kwota nie moze byc pusta")
    @Positive(message = "Kwota musi być dodatnia")
    private Double amount;

    @NotBlank(message = "Typ Transakcji nie moze byc pusty")
    @Pattern(regexp = "^(INCOME|EXPENSE)$", message = "Typ Transakcji musi być INCOME lub EXPENSE")
    private String type;

    @NotBlank(message = "Tytul nie moze byc pusty")
    @Size(max = 100, message = "Tytul nie moze byc dłuższy niż 100 znaków")
    private String title;

    private List<Long> selectedUserIds;

}
