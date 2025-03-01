package cardindex.dojocardindex.web.dto;

import cardindex.dojocardindex.User.models.*;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
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

    @Size(max = 500)
    private String interests;

    @Positive(message = "Въведената стойност трябва да бъде цяло, положително число")
    private int height;

    @Positive(message = "Въведената стойност трябва да бъде цяло, положително число")
    private int weight;

    private String contactPerson;

    @Size(min = 10, max = 15, message = "Въведете валиден телефонен номер.")
    private String contactPersonPhone;

}
