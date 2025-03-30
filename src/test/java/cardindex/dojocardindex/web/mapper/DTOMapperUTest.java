package cardindex.dojocardindex.web.mapper;

import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.models.EventType;
import cardindex.dojocardindex.Event.models.Requirements;
import cardindex.dojocardindex.User.models.*;
import cardindex.dojocardindex.web.dto.EditEventRequest;
import cardindex.dojocardindex.web.dto.EditUserProfileRequest;
import cardindex.dojocardindex.web.dto.UserEditAdminRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class DTOMapperUTest {

    @Test
    void given_happyPath_when_mapUserToEditUserRequest(){

        User user = User.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .userPhone("0888888888")
                .profilePicture("www.picdata.com")
                .birthDate(LocalDate.parse("1980-05-03"))
                .interests("none")
                .height(180)
                .weight(75)
                .contactPerson("Georgi Ivanov")
                .contactPersonPhone("0888888887")
                .build();

        EditUserProfileRequest dto = DTOMapper.mapUserToEditUserRequest(user);
        assertEquals(user.getFirstName(),(dto.getFirstName()));
        assertEquals(user.getLastName(),(dto.getLastName()));
        assertEquals(user.getUserPhone(),(dto.getUserPhone()));
        assertEquals(user.getProfilePicture(),(dto.getProfilePicture()));
        assertEquals(user.getInterests(),(dto.getInterests()));
        assertEquals(user.getHeight(),(dto.getHeight()));
        assertEquals(user.getWeight(),(dto.getWeight()));
        assertEquals(user.getContactPerson(),(dto.getContactPerson()));
        assertEquals(user.getContactPersonPhone(),(dto.getContactPersonPhone()));

    }

    @Test
    void given_mapEventToEditEventRequest_happyPath(){

        Event event = Event.builder()
                .id(UUID.randomUUID())
                .type(EventType.TOURNAMENT)
                .EventDescription("Тестов турнир").startDate(LocalDate.parse("05.05.2025", DateTimeFormatter.ofPattern("dd.MM.yyyy")))

                .endDate(LocalDate.parse("06.05.2025",DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                .location("Каспичан")
                .requirements(Requirements.NONE)
                .closed(false)
                .build();

        EditEventRequest dto = DTOMapper.mapEventToEditEventRequest(event);

        assertEquals(event.getType(),dto.getEventType());
        assertEquals(event.getEventDescription(),dto.getEventDescription());
        assertEquals(event.getStartDate(),dto.getStartDate());
        assertEquals(event.getEndDate(),dto.getEndDate());
        assertEquals(event.getLocation(),dto.getLocation());
        assertEquals(event.getRequirements(),dto.getRequirements());
    }

    @Test
    void given_mapUserToUserEditAdminRequest_happyPath(){

        User user = User.builder()
                .firstName("Ivan")
                .lastName("Ivanov")
                .userPhone("0888888888")
                .profilePicture("www.picdata.com")
                .birthDate(LocalDate.parse("1980-05-03"))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .reachedDegree(Degree.NONE)
                .ageGroup(AgeGroup.M)
                .isCompetitor(false)
                .medicalExamsPassed(LocalDate.parse("2025-02-02"))
                .interests("none")
                .height(180)
                .weight(75)
                .contactPerson("Georgi Ivanov")
                .contactPersonPhone("0888888887")
                .build();

        UserEditAdminRequest dto = DTOMapper.mapUserToUserEditAdminRequest(user);

        assertEquals(user.getBirthDate(),dto.getBirthDate());
        assertEquals(user.getUserPhone(),dto.getUserPhone());
        assertEquals(user.getRole(),dto.getRole());
        assertEquals(user.getStatus(),dto.getStatus());
        assertEquals(user.getRegistrationStatus(),dto.getRegistrationStatus());
        assertEquals(user.getReachedDegree(),dto.getReachedDegree());
        assertEquals(user.getIsCompetitor(),dto.getIsCompetitor());
        assertEquals(user.getAgeGroup(),dto.getAgeGroup());
        assertEquals(user.getHeight(),dto.getHeight());
        assertEquals(user.getWeight(),dto.getWeight());
        assertEquals(user.getMedicalExamsPassed(),dto.getMedicalExamsPassed());
        assertEquals(user.getContactPerson(),dto.getContactPerson());
        assertEquals(user.getContactPersonPhone(),dto.getContactPersonPhone());

    }




}
