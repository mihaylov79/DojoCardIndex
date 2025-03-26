package cardindex.dojocardindex.User;

import cardindex.dojocardindex.User.models.*;
import cardindex.dojocardindex.User.repository.UserRepository;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.exceptions.UserAlreadyExistException;
import cardindex.dojocardindex.exceptions.UserNotFoundException;
import cardindex.dojocardindex.notification.service.NotificationService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.CreateUserRequest;
import cardindex.dojocardindex.web.dto.EditUserProfileRequest;
import cardindex.dojocardindex.web.dto.RegisterRequest;
import cardindex.dojocardindex.web.dto.UserEditAdminRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceUTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private UserService userService;

    @Test
    void given_notExistingUserInDatabase_when_editUserProfile_then_exceptionIsThrown() {

        UUID userId = UUID.randomUUID();
        EditUserProfileRequest dto = EditUserProfileRequest.builder().build();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.editUserProfile(userId, dto));


    }

    @Test
    void given_ExistingUser_whenEditHisProfileDetails_then_DetailsAreChangedAndSavedToDatabase(){

        UUID userId = UUID.randomUUID();
        EditUserProfileRequest dto = EditUserProfileRequest.builder()
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

        User user = User.builder().build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.editUserProfile(userId,dto);

        assertEquals("Ivan",dto.getFirstName());
        assertEquals("Ivanov",dto.getLastName());
        assertEquals("0888888888",dto.getUserPhone());
        assertEquals("www.picdata.com",dto.getProfilePicture());
        assertEquals("none",dto.getInterests());
        assertEquals(180,dto.getHeight());
        assertEquals(75,dto.getWeight());
        assertEquals("Georgi Ivanov",dto.getContactPerson());
        assertEquals("0888888887",dto.getContactPersonPhone());
        verify(userRepository,times(1)).save(user);

    }

    @Test
    void given_nonExistingUser_when_loadUserByUsername_then_throwException(){

        String email = "abv@231.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.loadUserByUsername(email));

    }

    @Test
    void given_existingUser_when_loadUserByUsername_then_return_validCustomUserDetails(){

        String email = "abv@231.com";
        User user = User.builder()
                .id(UUID.randomUUID())
                .email(email)
                .password("123321")
                .role(UserRole.ADMIN)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        UserDetails customUserDetails = userService.loadUserByUsername(email);

        assertInstanceOf(CustomUserDetails.class, customUserDetails);
        CustomUserDetails result = (CustomUserDetails) customUserDetails;

        assertEquals(user.getId(),result.getId());
        assertEquals(user.getEmail(),result.getEmail());
        assertEquals(user.getPassword(),result.getPassword());
        assertEquals(user.getRole(),result.getRole());
        assertEquals(user.getRegistrationStatus(),result.getRegistrationStatus());
        assertEquals(user.getStatus(),result.getUserStatus());
        assertEquals(1,result.getAuthorities().size());
        assertEquals("ROLE_ADMIN", result.getAuthorities().iterator().next().getAuthority());


    }

    @Test
    void given_NonExistingUser_when_register_then_throwUserNotFoundException(){

        RegisterRequest dto = RegisterRequest.builder()
                .email("ivan@home.bg")
                .password("123321")
                .firstName("Ivan")
                .lastName("Ivanov")
                .build();

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,() ->userService.register(dto));
        verify(userRepository, never()).save(any());

    }

    @Test
    void given_ExistingUserIsINACTIVE_when_register_then_ThrowUserAlreadyExistException(){

        RegisterRequest dto = RegisterRequest.builder()
                .email("ivan@home.bg")
                .password("123321")
                .firstName("Ivan")
                .lastName("Ivanov")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("ivan@home.bg")
                .password("123321")
                .firstName("Ivan")
                .lastName("Ivanov")
                .reachedDegree(Degree.NONE)
                .registrationStatus(RegistrationStatus.NOT_REGISTERED)
                .status(UserStatus.INACTIVE)
                .build();

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));

        assertThrows(UserAlreadyExistException.class, () -> userService.register(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void given_ExistingUserIsREGISTERED_when_register_then_throwUserAlreadyExistException(){

        RegisterRequest dto = RegisterRequest.builder()
                .email("ivan@home.bg")
                .password("123321")
                .firstName("Ivan")
                .lastName("Ivanov")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("ivan@home.bg")
                .password("123321")
                .firstName("Ivan")
                .lastName("Ivanov")
                .reachedDegree(Degree.NONE)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));

        assertThrows(UserAlreadyExistException.class, () -> userService.register(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void given_ExistingUserIsPENDING_when_register_then_throwUserAlreadyExistException(){

        RegisterRequest dto = RegisterRequest.builder()
                .email("ivan@home.bg")
                .password("123321")
                .firstName("Ivan")
                .lastName("Ivanov")
                .build();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("ivan@home.bg")
                .password("123321")
                .firstName("Ivan")
                .lastName("Ivanov")
                .reachedDegree(Degree.NONE)
                .registrationStatus(RegistrationStatus.PENDING)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(user));

        assertThrows(UserAlreadyExistException.class, () -> userService.register(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void given_ExistingUserIsNOT_REGISTERED_when_register_then_registerUser(){

        RegisterRequest dto = RegisterRequest.builder()
                .email("ivan@home.bg")
                .password("123321")
                .firstName("Ivan")
                .lastName("Ivanov")
                .build();

        User existingUser = User.builder()
                .id(UUID.randomUUID())
                .email("ivan@home.bg")
                .password("123321")
                .firstName("Ivan")
                .lastName("Ivanov")
                .reachedDegree(Degree.NONE)
                .registrationStatus(RegistrationStatus.NOT_REGISTERED)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode(dto.getPassword())).thenReturn("encodedPassword");
        userService.register(dto);

        verify(userRepository, times(1)).save(Mockito.argThat(user ->
                user.getEmail().equals(dto.getEmail()) &&
                        user.getPassword().equals("encodedPassword") &&
                        user.getRegistrationStatus() == RegistrationStatus.PENDING));

    }

   @Test
    void given_ExistingUsersInDatabase_when_getAllUsers_then_returnAllUsersInDB(){

       List<User> dbUsers = List.of(new User(), new User());


       when(userRepository.findAll(Sort.by(Sort.Order.desc("registrationStatus"),Sort.Order.desc("status")))).thenReturn(dbUsers);

       List<User>users = userService.getAllUsers();

       assertEquals(2,users.size());


   }

   @Test
    void given_invalidUserId_when_approveRequest_then_throw_userNotFoundException(){

        UUID userId = UUID.randomUUID();

        assertThrows(UserNotFoundException.class,()->userService.getUserById(userId));

   }

   @Test
    void given_existingUser_when_approveRequest_then_updateRegistrationStatusAndSendNotification(){

       UUID userId = UUID.randomUUID();
        User existingUser = User.builder()
                .id(userId)
                .email("ivan@example.com")
                .firstName("Ivan")
                .lastName("Ivanov")
                .registrationStatus(RegistrationStatus.PENDING)
                .status(UserStatus.ACTIVE)
                .build();

       when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        userService.approveRequest(userId);

        verify(userRepository,times(1)).save(Mockito.argThat(user -> user.getRegistrationStatus()== RegistrationStatus.REGISTERED));
        verify(notificationService,times(1)).saveNotificationPreference(userId,true,existingUser.getEmail());
        verify(notificationService,times(1)).sendNotification(userId,existingUser.getFirstName(),existingUser.getLastName(),"Одобрена заявка за регистрация","Вашата заявка за регистрация беше потвърдена.Вече можете да влезете в профила си.");

   }

    @Test
    void given_existingUser_when_denyRequest_then_updateRegistrationStatusAndSaveUserToDb(){

        UUID userId = UUID.randomUUID();
        User existingUser = User.builder()
                .id(userId)
                .email("ivan@example.com")
                .firstName("Ivan")
                .lastName("Ivanov")
                .registrationStatus(RegistrationStatus.PENDING)
                .status(UserStatus.ACTIVE)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));

        userService.denyRequest(userId);

        verify(userRepository,times(1)).save(Mockito.argThat(user -> user.getRegistrationStatus()== RegistrationStatus.NOT_REGISTERED));

    }


    @Test
    void given_ExistingUser_whenEditUserProfileByAdmin_then_DetailsAreChangedAndSavedToDatabase(){

        UUID userId = UUID.randomUUID();
        UserEditAdminRequest dto = UserEditAdminRequest.builder()
                .userPhone("0888888889")
                .birthDate(LocalDate.parse("1980-05-03"))
                .role(UserRole.ADMIN)
                .isCompetitor(false)
                .status(UserStatus.ACTIVE)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .reachedDegree(Degree.NONE)
                .ageGroup(AgeGroup.M)
                .height(180)
                .weight(75)
                .medicalExamsPassed(LocalDate.parse("2025-02-05"))
                .contactPerson("Georgi Ivanov")
                .contactPersonPhone("0888888860")
                .build();

        User user = User.builder()
                .id(userId)
                .userPhone("0888888886")
                .birthDate(LocalDate.parse("1995-05-03"))
                .role(UserRole.ADMIN)
                .isCompetitor(true)
                .status(UserStatus.ACTIVE)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .reachedDegree(Degree.KYU_4)
                .ageGroup(AgeGroup.CH14)
                .height(175)
                .weight(70)
                .medicalExamsPassed(LocalDate.parse("2025-01-05"))
                .contactPerson("Gergana Ivanova")
                .contactPersonPhone("0888888882")
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        userService.editUserProfileByAdmin(userId,dto);


        verify(userRepository, times(1)).save(Mockito.argThat(savedUser ->
                        savedUser.getUserPhone().equals(dto.getUserPhone()) &&
                        savedUser.getBirthDate().equals(dto.getBirthDate()) &&
                        savedUser.getRole().equals(dto.getRole()) &&
                        savedUser.getIsCompetitor()==(dto.getIsCompetitor()) &&
                        savedUser.getStatus().equals(dto.getStatus()) &&
                        savedUser.getRegistrationStatus().equals(dto.getRegistrationStatus()) &&
                        savedUser.getReachedDegree().equals(dto.getReachedDegree()) &&
                        savedUser.getAgeGroup().equals(dto.getAgeGroup()) &&
                        savedUser.getHeight() == dto.getHeight() &&
                        savedUser.getWeight() == dto.getWeight() &&
                        savedUser.getMedicalExamsPassed().equals(dto.getMedicalExamsPassed()) &&
                        savedUser.getContactPerson().equals(dto.getContactPerson()) &&
                        savedUser.getContactPersonPhone().equals(dto.getContactPersonPhone())
        ));

    }


    @Test
    void given_NonExistingUser_createNewUser_then_newUserAddedAndSavedToDatabase(){

        UUID userId = UUID.randomUUID();
        CreateUserRequest dto = CreateUserRequest.builder()
                .email("ivan@home.bg")
                .firstName("Ivan")
                .lastName("Ivanov")
                .userPhone("0888888888")
                .profilePicture("www.pictureData.com")
                .birthDate(LocalDate.parse("1980-05-03"))
                .role(UserRole.ADMIN)
                .isCompetitor(false)
                .reachedDegree(Degree.NONE)
                .ageGroup(AgeGroup.M)
                .height(180)
                .weight(75)
                .contactPerson("Georgi Ivanov")
                .build();

        User user = User.builder().id(userId).build();
        when(userRepository.findByEmail(dto.getEmail())).thenReturn(Optional.empty());

        userService.createNewUser(dto);

        verify(userRepository, times(1)).save(Mockito.argThat(savedUser ->
                        savedUser.getEmail().equals(dto.getEmail()) &&
                        savedUser.getFirstName().equals(dto.getFirstName()) &&
                        savedUser.getLastName().equals(dto.getLastName()) &&
                        savedUser.getUserPhone().equals(dto.getUserPhone()) &&
                        savedUser.getProfilePicture().equals(dto.getProfilePicture()) &&
                        savedUser.getBirthDate().equals(dto.getBirthDate()) &&
                        savedUser.getRole().equals(dto.getRole()) &&
                        savedUser.getIsCompetitor() == dto.getIsCompetitor() &&
                        savedUser.getReachedDegree().equals(dto.getReachedDegree()) &&
                        savedUser.getAgeGroup().equals(dto.getAgeGroup()) &&
                        savedUser.getHeight()==(dto.getHeight()) &&
                        savedUser.getWeight()==(dto.getWeight()) &&
                        savedUser.getContactPerson().equals(dto.getContactPerson())
        ));

    }


}
