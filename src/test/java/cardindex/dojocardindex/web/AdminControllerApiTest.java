package cardindex.dojocardindex.web;

import cardindex.dojocardindex.User.models.*;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.CreateUserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;



@WebMvcTest(AdminController.class)
public class AdminControllerApiTest {

    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;


    @Test
    @WithMockUser(roles = "ADMIN")
    void getAddNewUserPage_WithAdminRole_ReturnsAddUserView() throws Exception {

        UUID id = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(id, "admin@test.com", "pass",
                UserRole.ADMIN, RegistrationStatus.REGISTERED,UserStatus.ACTIVE);

        User mockUser = User.builder().id(id).build();

        when(userService.getUserById(mockUser.getId())).thenReturn(mockUser);


        mockMvc.perform(get("/admin/add-user")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("addUser"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("createUserRequest"))
                .andExpect(model().attribute("user", mockUser));
    }

    @Test
    public void addNewUser_WithValidationErrors_ShouldReturnAddUserView() throws Exception {

        CreateUserRequest invalidRequest = new CreateUserRequest();

        UUID userId = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(
                userId,
                "admin@test.com",
                "pass",
                UserRole.ADMIN,
                RegistrationStatus.REGISTERED,
                UserStatus.ACTIVE
        );

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        User mockUser = User.builder().id(userId).build();
        when(userService.getUserById(userId)).thenReturn(mockUser);

        mockMvc.perform(post("/admin/add-user")
                        .with(csrf())
                        .flashAttr("createUserRequest", invalidRequest))
                .andExpect(status().isOk())
                .andExpect(view().name("addUser"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("org.springframework.validation.BindingResult.createUserRequest"));


        SecurityContextHolder.clearContext();
    }

    @Test
    public void addNewUser_WithAdminRoleAndValidData_ShouldRedirectToHome() throws Exception {
        // 1. Подготовка на тестови данни
        UUID userId = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(
                userId,
                "admin@test.com",
                "pass",
                UserRole.ADMIN,
                RegistrationStatus.REGISTERED,
                UserStatus.ACTIVE
        );

        // 2. Подготовка на заявката
        CreateUserRequest request = new CreateUserRequest();
        request.setEmail("test@example.com");
        request.setFirstName("Иван");
        request.setLastName("Иванов");
        request.setRole(UserRole.ADMIN);
        request.setUserPhone("0876333222");
        request.setBirthDate(LocalDate.parse("1981-03-05"));
        request.setReachedDegree(Degree.NONE);
        request.setInterests("Няма");
        request.setIsCompetitor(false);
        request.setHeight(176);
        request.setWeight(70);
        request.setMedicalExamsPassed(LocalDate.parse("2025-03-01"));
        request.setContactPerson("Георги Иванов");
        request.setContactPersonPhone("0877322122");


        // 3. Мокване на услугите
        User mockUser = User.builder().id(userId).build();
        when(userService.getUserById(userId)).thenReturn(mockUser);
        doNothing().when(userService).createNewUser(any(CreateUserRequest.class));

        // 4. Настройка на Security Context
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        // 5. Изпълнение и проверки
        mockMvc.perform(post("/admin/add-user")
                        .with(user(userDetails))
                        .with(csrf())
                        .flashAttr("createUserRequest", request))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        // 6. Допълнителна проверка за извикване на услугата
        verify(userService, times(1)).createNewUser(any(CreateUserRequest.class));

        // 7. Почистване
        SecurityContextHolder.clearContext();
    }

}
