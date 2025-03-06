package cardindex.dojocardindex.web.dto;

import cardindex.dojocardindex.User.models.*;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;
import org.springframework.format.annotation.DateTimeFormat;


import java.time.LocalDate;
@Data
public class CreateUserRequest {

    @NotNull
    @Email(message = "Въведете валиден мейл адрес")
    private String email;

    @NotNull(message = "Това поле не може да бъде празно")
    private UserRole role;

    @Size(max = 20, message = "Името не може да надвишава 20 симовла")
    private String firstName;

    @Size(max = 20, message = "Името не може да надвишава 20 симовла")
    private String lastName;

    @Size(min = 10, max = 15, message = "Въведете валиден телефонен номер")
    private String userPhone;

    @URL(message = "Моля въведета валиден URL")
    private String profilePicture;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;



    private Degree reachedDegree;

   @Length(max = 500, message = "Това поле не може да надвишава 500 символа")
    private String interests;

    private AgeGroup ageGroup;

    @NotNull(message = "Моля отбележете дали потребителят е състезател")
    private boolean isCompetitor;


    private int height;


    private int weight;

    private LocalDate medicalExamsPassed;

    private String contactPerson;

    private String contactPersonPhone;


    public boolean getIsCompetitor() {
        return isCompetitor;
    }

    public void setIsCompetitor(boolean isCompetitor) {
        this.isCompetitor = isCompetitor;
    }

}
