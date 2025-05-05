package cardindex.dojocardindex.web.dto;


import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
public class EditUserProfileRequest {

    @Size(min = 2, max = 20, message = "Въведеното име трябва да е между 2 и 20 символа!")
    private String firstName;
    @Size(min = 2, max = 20, message = "Въведеното име трябва да е между 2 и 20 символа!")
    private String lastName;

    @Size(min = 10, max = 15, message = "Въведете валиден телефонен номер.")
    private String userPhone;

    @URL(message = "въведете валиден URL адрес.")
    private String profilePicture;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @Size(max = 500, message = "Това поле не може да съдържа повече от 500 символа")
    private String interests;

    @Positive(message = "Въведената стойност трябва да бъде цяло, положително число")
    private double height;

    @Positive(message = "Въведената стойност трябва да бъде цяло, положително число")
    private double weight;

    private String contactPerson;

    @Size(min = 10, max = 15, message = "Въведете валиден телефонен номер.")
    private String contactPersonPhone;

    public void setUserPhone(String userPhone) {
        this.userPhone = (userPhone.isBlank()) ? null :userPhone;
    }

    public void setContactPersonPhone(String contactPersonPhone) {
        this.contactPersonPhone = (contactPersonPhone.isBlank()) ? null : contactPersonPhone;
    }

}
