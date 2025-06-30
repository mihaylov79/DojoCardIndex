package cardindex.dojocardindex.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Това поле не може да бъде празно")
    @Email
    private String email;

    @NotBlank(message = "Това поле не може да бъде празно")
    @Size(min = 4, max = 20, message = "Паролата трябва да бъде между 4 и 20 символа!")
    private String password;
}
