package cardindex.dojocardindex.web.mapper;

import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.web.dto.CreateEventRequest;
import cardindex.dojocardindex.web.dto.EditUserProfileRequest;
import cardindex.dojocardindex.web.dto.UserEditAdminRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DTOMapper {

    public static EditUserProfileRequest mapUserToEditUserRequest (User user){

            return EditUserProfileRequest.builder()
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .userPhone(user.getUserPhone())
                    .profilePicture(user.getProfilePicture())
                    .birthDate(user.getBirthDate())
                    .interests(user.getInterests())
                    .height(user.getHeight())
                    .weight(user.getWeight())
                    .contactPerson(user.getContactPerson())
                    .contactPersonPhone(user.getContactPersonPhone())
                    .build();
    }


    public static UserEditAdminRequest mapUserToUserEditAdminRequest(User user) {


        return UserEditAdminRequest.builder()
                .birthDate(user.getBirthDate())
                .userPhone(user.getUserPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .registrationStatus(user.getRegistrationStatus())
                .reachedDegree(user.getReachedDegree())
                .isCompetitor(user.getIsCompetitor() != true ? false : true)
                .ageGroup(user.getAgeGroup())
                .height(user.getHeight())
                .weight(user.getWeight())
                .medicalExamsPassed(user.getMedicalExamsPassed())
                .contactPerson(user.getContactPerson())
                .contactPersonPhone(user.getContactPersonPhone())
                .build();
    }

    public static CreateEventRequest mapEventToCreateEventRequest(Event event) {

        return CreateEventRequest.builder()
                .eventType(event.getType())
                .eventDescription(event.getEventDescription())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .requirements(event.getRequirements())
                .build();
    }
}
