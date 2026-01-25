package cardindex.dojocardindex.web.dto;

import cardindex.dojocardindex.User.models.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
public class UserEditAdminRequest {

//    @Size(min = 10, max = 15, message = "Въведете валиден телефонен номер.")
    @Pattern(regexp = "^$|^(?:\\+359|0)?[0-9]{8,9}$", message = "Невалиден телефонен номер")
    private String userPhone;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @NotNull(message = "Това поле не може да бъде празно")
    private UserRole role;


    private boolean isCompetitor;

    private UserStatus status;

    private RegistrationStatus registrationStatus;

    private Degree reachedDegree;

    private AgeGroup ageGroup;

    @PositiveOrZero
    private double height;

    @PositiveOrZero
    private double weight;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate medicalExamsPassed;

    @Size(max = 40, message = "Името не трябва да надвишава 40 символа.")
    private String contactPerson;

//    @Size(min = 10, max = 15, message = "Въведете валиден телефонен номер.")
    @Pattern(regexp = "^$|^(?:\\+359|0)?[0-9]{8,9}$", message = "Невалиден телефонен номер")
    private String contactPersonPhone;


    public boolean getIsCompetitor() {
        return isCompetitor;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = (userPhone.isBlank()) ? null :userPhone;
    }

    public void setContactPersonPhone(String contactPersonPhone) {
        this.contactPersonPhone = (contactPersonPhone.isBlank()) ? null : contactPersonPhone;
    }

}


