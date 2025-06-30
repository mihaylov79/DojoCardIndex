package cardindex.dojocardindex.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    @NotBlank(message = "Това поле не може да бъде празно")
    @Size(min = 4, max = 20, message = "Паролата трябва дад бъде между 4 и 20 символа!")
    private String oldPassword;

    @NotBlank(message = "Това поле не може да бъде празно")
    @Size(min = 4, max = 20, message = "Паролата трябва дад бъде между 4 и 20 символа!")
    private String newPassword;
}
