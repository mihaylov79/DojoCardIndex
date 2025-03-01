package cardindex.dojocardindex.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Component;
import org.unbescape.xml.XmlEscape;


public class RegisterRequest {

    @Email(message = "Въведете валиден адрес на електронна поща")
    @NotBlank(message = "Това поле не може да бъде празно")
    private String email;

    @Size(min = 2, max = 20, message = "Въведеното име трябва да е между 2 и 20 символа!")
    private String firstName;

    @Size(min = 2, max = 20, message = "Въведеното име трябва да е между 2 и 20 символа!")
    private String LastName;

    @NotBlank(message = "Това поле не може да бъде празно")
    @Size(min = 4, max = 20, message = "Паролата трябва дад бъде между 4 и 20 символа!")
    private String password;


    public RegisterRequest() {

    }

    public RegisterRequest(String email, String firstName, String lastName, String password) {
        this.email = email;
        this.firstName = firstName;
        LastName = lastName;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return LastName;
    }

    public void setLastName(String lastName) {
        LastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
