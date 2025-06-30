package cardindex.dojocardindex.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgottenPasswordEmailRequest {

    @NotBlank(message = "Това поле не може да бъде празно")
    @Email(message = "Въведете валиден имейл")
    private String Email;
}
