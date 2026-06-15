package pk.gp.pasir_galusza_pawel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BalanceDto {

    @Min(value = 0, message = "przychod musi być wiekszy lub rowny 0")
    private Double totalIncome;

    @Min(value = 0, message = "wydatki musza być wieksze lub rowne 0")
    private Double totalExpense;

    @NotNull(message = "balans musi miec wartosc")
    private Double balance;
}
