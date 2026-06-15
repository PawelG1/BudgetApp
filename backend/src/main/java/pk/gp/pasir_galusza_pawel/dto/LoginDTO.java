package pk.gp.pasir_galusza_pawel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginDTO {

    @NotBlank(message = "Adres e-mail jest wymagany")
    private String email;

    @NotBlank(message = "Haslo jest wymagane")
    private String password;

}



