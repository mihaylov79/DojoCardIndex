package cardindex.dojocardindex.web.dto;

import cardindex.dojocardindex.User.models.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@Builder
public class UserEditAdminRequest {

    @Size(min = 10, max = 15, message = "Въведете валиден телефонен номер.")
    private String userPhone;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private UserRole role;

    private boolean isCompetitor;

    private UserStatus status;

    private RegistrationStatus registrationStatus;

    private Degree reachedDegree;

    private AgeGroup ageGroup;

    private int height;

    private int weight;

    private LocalDate medicalExamsPassed;

    private String contactPerson;

    @Size(min = 10, max = 15, message = "Въведете валиден телефонен номер.")
    private String contactPersonPhone;


    public boolean getIsCompetitor() {
        return isCompetitor;
    }

}


