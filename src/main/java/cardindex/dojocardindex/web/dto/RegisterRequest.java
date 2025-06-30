package cardindex.dojocardindex.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class RegisterRequest {

    @Email(message = "Въведете валиден адрес на електронна поща")
    @NotBlank(message = "Това поле не може да бъде празно")
    private String email;

    @Size(min = 2, max = 20, message = "Въведеното име трябва да е между 2 и 20 символа!")
    private String firstName;

    @Size(min = 2, max = 20, message = "Въведеното име трябва да е между 2 и 20 символа!")
    private String lastName;

    @NotBlank(message = "Това поле не може да бъде празно")
    @Size(min = 4, max = 20, message = "Паролата трябва дад бъде между 4 и 20 символа!")
    private String password;


    public RegisterRequest() {

    }

    public RegisterRequest(String email, String firstName, String lastName, String password) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.password = password;
    }

}
