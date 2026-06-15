package pk.gp.pasir_galusza_pawel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DebtDTO {

    @NotNull(message = "Id dluznika nie moze byc puste")
    private Long debtorId;

    @NotNull(message = "Id wierzyciela nie moze byc puste")
    private Long creditorId;

    @NotNull(message = "Id grupy nie moze byc puste")
    private Long groupId;

    @NotNull(message = "Kwota nie moze byc pusta")
    @Positive(message = "Kwota musi byc wieksza od zera")
    private Double amount;

    @NotBlank(message = "Tytul nie moze byc pusty")
    @Size(max = 100, message = "Tytul nie mzoe przekraczac 100 znakow")
    private String title;
}
