package cardindex.dojocardindex.User;

import cardindex.dojocardindex.User.models.Degree;
import cardindex.dojocardindex.User.models.RegistrationStatus;
import cardindex.dojocardindex.User.models.UserRole;
import cardindex.dojocardindex.User.repository.UserRepository;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.web.dto.CreateUserRequest;
import cardindex.dojocardindex.web.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;


import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
public class RegisterUserITest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void registerUser_happyPath(){

        CreateUserRequest userDto= CreateUserRequest.builder()
                .email("user1@examplez.com")
                .role(UserRole.ADMIN)
                .reachedDegree(Degree.NONE)
                .firstName("Ivan")
                .lastName("Ivanov").build();

        RegisterRequest registerDTO = RegisterRequest.builder()
                .email("user1@examplez.com")
                .firstName("Ivan")
                .lastName("Ivanov")
                .password("123321").build();

        userService.createNewUser(userDto);
        userService.register(registerDTO);
        userService.approveRequest(userRepository.getByEmail("user1@examplez.com").getId());

        //очакваме 2-ма потребители - защото базата създава admin@example.com по подразбиране!
        assertEquals(2,userRepository.findAll().size());
        assertEquals(RegistrationStatus.REGISTERED, userRepository.getByEmail("user1@examplez.com").getRegistrationStatus());




    }


}
