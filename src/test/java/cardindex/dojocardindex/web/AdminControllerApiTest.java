package cardindex.dojocardindex.web;

import cardindex.dojocardindex.User.models.RegistrationStatus;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.models.UserRole;
import cardindex.dojocardindex.User.models.UserStatus;
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

import java.util.UUID;

import static org.mockito.Mockito.when;
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
    @WithMockUser(roles = "ADMIN")
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
}
