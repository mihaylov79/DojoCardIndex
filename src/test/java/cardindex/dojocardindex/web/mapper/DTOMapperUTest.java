package cardindex.dojocardindex.web.mapper;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.web.dto.EditUserProfileRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

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
}
