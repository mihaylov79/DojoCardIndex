package cardindex.dojocardindex.web;

import cardindex.dojocardindex.User.models.*;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.CreateUserRequest;
import cardindex.dojocardindex.web.dto.UserEditAdminRequest;
import cardindex.dojocardindex.web.mapper.DTOMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.*;

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

        UUID userId = UUID.randomUUID();
        CustomUserDetails userDetails = new CustomUserDetails(
                userId,
                "admin@test.com",
                "pass",
                UserRole.ADMIN,
                RegistrationStatus.REGISTERED,
                UserStatus.ACTIVE
        );

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

        User mockUser = User.builder().id(userId).build();
        when(userService.getUserById(userId)).thenReturn(mockUser);
        doNothing().when(userService).createNewUser(any(CreateUserRequest.class));

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        mockMvc.perform(post("/admin/add-user")
                        .with(user(userDetails))
                        .with(csrf())
                        .flashAttr("createUserRequest", request))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        verify(userService, times(1)).createNewUser(any(CreateUserRequest.class));

        SecurityContextHolder.clearContext();
    }

    @Test
    public void getRegisterRequests_WithAdminRole_ShouldReturnViewWithRequests() throws Exception {

       UUID detailsId = UUID.randomUUID();
       CustomUserDetails userDetails = new CustomUserDetails(
               detailsId,
               "admin@example.com",
               "123321",
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


        User mockUser = User.builder()
                .id(detailsId)
                .email("admin@example.com")
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .build();
        User request1 = User.builder()
                .email("user1@test.com")
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .build();
        User request2 = User.builder()
                .email("user2@test.com")
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .build();
        List<User> mockRequests = Arrays.asList(request1, request2);

        when(userService.getUserById(detailsId)).thenReturn(mockUser);
        when(userService.getRegisterRequests()).thenReturn(mockRequests);

        mockMvc.perform(get("/admin/register-requests"))
                .andExpect(status().isOk())
                .andExpect(view().name("register-requests"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attribute("user", mockUser))
                .andExpect(model().attributeExists("registerRequests"))
                .andExpect(model().attribute("registerRequests", mockRequests));
    }

    @Test
    public void approveRegisterRequest_WithAdminRole_ShouldApproveRequestAndRedirect() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        CustomUserDetails adminDetails = new CustomUserDetails(
                adminId,
                "admin@example.com",
                "password",
                UserRole.ADMIN,
                RegistrationStatus.REGISTERED,
                UserStatus.ACTIVE
        );

        User adminUser = User.builder()
                .id(adminId)
                .email("admin@example.com")
                .role(UserRole.ADMIN)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .build();

        User requestUser = User.builder()
                .id(requestId)
                .email("user@test.com")
                .role(UserRole.MEMBER)
                .registrationStatus(RegistrationStatus.PENDING)
                .status(UserStatus.ACTIVE)
                .build();

        when(userService.getUserById(adminId)).thenReturn(adminUser);
        when(userService.getUserById(requestId)).thenReturn(requestUser);
        doNothing().when(userService).approveRequest(requestId);

        mockMvc.perform(post("/admin/register-requests/approve")
                        .param("id", requestId.toString())
                        .with(user(adminDetails))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/register-requests"))
                .andExpect(model().attributeExists("currentUser"));

        verify(userService, times(1)).approveRequest(requestId);
    }




    @Test
    @WithMockUser(roles = "ADMIN")
    public void denyRegisterRequest_AdminRole_ShouldDenyAndRedirect() throws Exception {
        // Arrange
        UUID adminId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        CustomUserDetails adminDetails = new CustomUserDetails(
                adminId, "admin@test.com", "pass",
                UserRole.ADMIN, RegistrationStatus.REGISTERED, UserStatus.ACTIVE);

        User adminUser = User.builder()
                .id(adminId)
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .build();

        User requestUser = User.builder()
                .id(requestId)
                .email("user@test.com")
                .role(UserRole.MEMBER)
                .registrationStatus(RegistrationStatus.PENDING)
                .status(UserStatus.ACTIVE)
                .build();

        when(userService.getUserById(adminId)).thenReturn(adminUser);
        when(userService.getUserById(requestId)).thenReturn(requestUser);
        doNothing().when(userService).denyRequest(requestId);


        mockMvc.perform(post("/admin/register-requests/deny")
                        .param("id", requestId.toString())
                        .with(user(adminDetails))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/register-requests"))
                .andExpect(model().attributeExists("currentUser"));

        verify(userService, times(1)).denyRequest(requestId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void denyRegisterRequest_InvalidId_ShouldReturnError() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/admin/register-requests/deny")
                        .param("id", "invalid-uuid")
                        .with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void getAllUsersPage_AdminRole_ShouldReturnViewWithUsers() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();

        CustomUserDetails userDetails = new CustomUserDetails(
                userId,
                "admin@test.com",
                "encodedPassword",
                UserRole.ADMIN,
                RegistrationStatus.REGISTERED,
                UserStatus.ACTIVE
        );

        User mockUser = User.builder()
                .id(userId)
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .build();

        List<User> mockUsers = Arrays.asList(
                User.builder().id(UUID.randomUUID())
                        .email("user1@test.com")
                        .role(UserRole.MEMBER)
                        .registrationStatus(RegistrationStatus.REGISTERED)
                        .status(UserStatus.ACTIVE)
                        .build(),
                User.builder().id(UUID.randomUUID()).email("user2@test.com")
                        .role(UserRole.MEMBER)
                        .registrationStatus(RegistrationStatus.REGISTERED)
                        .status(UserStatus.ACTIVE)
                        .build()
        );

        Map<UUID, Integer> mockAges = new HashMap<>();
        mockAges.put(mockUsers.get(0).getId(), 25);
        mockAges.put(mockUsers.get(1).getId(), 30);

        when(userService.getUserById(userId)).thenReturn(mockUser);
        when(userService.getAllUsers()).thenReturn(mockUsers);
        when(userService.getUserAges(mockUsers)).thenReturn(mockAges);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        MockHttpServletRequestBuilder request = get("/admin/users/list/all-users").with(user(userDetails));

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("all-users-list"))
                .andExpect(model().attribute("user",mockUser))
                .andExpect(model().attributeExists("allUsers"))
                .andExpect(model().attributeExists("userAges"))
                .andExpect(model().attribute("allUsers", mockUsers))
                .andExpect(model().attribute("userAges", mockAges));

        verify(userService, times(1)).getUserById(userId);
        verify(userService, times(1)).getAllUsers();
        verify(userService, times(1)).getUserAges(mockUsers);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void modifyUserStatus_AdminRole_ShouldModifyAndRedirectBack() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        CustomUserDetails adminDetails = new CustomUserDetails(
                adminId,
                "admin@test.com",
                "encodedPassword",
                UserRole.ADMIN,
                RegistrationStatus.REGISTERED,
                UserStatus.ACTIVE
        );

        User adminUser = User.builder()
                .id(adminId)
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .reachedDegree(Degree.NONE)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .build();

        when(userService.getUserById(adminId)).thenReturn(adminUser);
        doNothing().when(userService).modifyAccStatus(targetUserId);

        String refererUrl = "/previous/page";

        mockMvc.perform(post("/admin/users/modify-status")
                        .with(user(adminDetails))
                        .param("id", targetUserId.toString())
                        .header("Referer", refererUrl)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(refererUrl))
                .andExpect(model().attributeExists("currentUser"));

        verify(userService, times(1)).modifyAccStatus(targetUserId);
        verify(userService, times(1)).getUserById(adminId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void modifyUserStatus_InvalidId_ShouldReturnError() throws Exception {
        mockMvc.perform(post("/admin/users/modify-status")
                        .param("id", "invalid-uuid")
                        .with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void getEditUserDetailsByAdminPage_AdminRole_ShouldReturnEditView() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        CustomUserDetails adminDetails = new CustomUserDetails(
                adminId,
                "admin@test.com",
                "encodedPassword",
                UserRole.ADMIN,
                RegistrationStatus.REGISTERED,
                UserStatus.ACTIVE
        );

        User adminUser = User.builder()
                .id(adminId)
                .email("admin@test.com")
                .role(UserRole.ADMIN)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .build();

        User targetUser = User.builder()
                .id(targetUserId)
                .email("user@test.com")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.MEMBER)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .build();

        UserEditAdminRequest expectedRequest = DTOMapper.mapUserToUserEditAdminRequest(targetUser);

        when(userService.getUserById(adminId)).thenReturn(adminUser);
        when(userService.getUserById(targetUserId)).thenReturn(targetUser);

        mockMvc.perform(get("/admin/users/details/edit/" + targetUserId)
                        .with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-user-edit"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("adminUser"))
                .andExpect(model().attributeExists("userEditAdminRequest"))
                .andExpect(model().attribute("user", targetUser))
                .andExpect(model().attribute("adminUser", adminUser))
                .andExpect(model().attribute("userEditAdminRequest", expectedRequest));

        verify(userService, times(1)).getUserById(adminId);
        verify(userService, times(1)).getUserById(targetUserId);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void getEditUserDetailsByAdminPage_NonExistingUser_ShouldReturnError() throws Exception {

        UUID nonExistingId = UUID.randomUUID();
        when(userService.getUserById(nonExistingId)).thenReturn(null);

        mockMvc.perform(get("/users/details/edit/" + nonExistingId))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void editUserDetailsByAdmin_ValidRequest_ShouldRedirect() throws Exception {

        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        CustomUserDetails adminDetails = new CustomUserDetails(
                adminId,
                "admin@test.com",
                "encodedPassword",
                UserRole.ADMIN,
                RegistrationStatus.REGISTERED,
                UserStatus.ACTIVE
        );

        User adminUser = User.builder()
                .id(adminId)
                .email("admin@test.com")
                .reachedDegree(Degree.NONE)
                .role(UserRole.ADMIN)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .build();

        User targetUser = User.builder()
                .id(targetUserId)
                .email("user@test.com")
                .firstName("Ivan")
                .lastName("Ivanov")
                .reachedDegree(Degree.NONE)
                .role(UserRole.MEMBER)
                .isCompetitor(true)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .userPhone("0876989898")
                .contactPersonPhone("0877788778")
                .build();


        when(userService.getUserById(adminId)).thenReturn(adminUser);
        when(userService.getUserById(targetUserId)).thenReturn(targetUser);
        doNothing().when(userService).editUserProfileByAdmin(any(), any());

        MockHttpServletRequestBuilder request = post("/admin/users/details/edit/{id}", targetUserId)
                .with(user(adminDetails))
                .formField("userPhone","0877321123")
                .formField("birthDate","1982-05-04")
                .formField("isCompetitor","true")
                .formField("reachedDegree", "NONE")
                .formField("ageGroup","M")
                .formField("height","180")
                .formField("weight","80")
                .formField("contactPerson","Georgi Ivanov")
                .formField("contactPersonPhone","0875888666")
                .formField("role", "MEMBER") // Добавете всички полета от UserEditAdminRequest
                .formField("registrationStatus", "REGISTERED")
                .formField("status", "ACTIVE")
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users/list"))
                .andExpect(model().attributeExists("user"));

        verify(userService, times(1)).editUserProfileByAdmin(eq(targetUserId), any());
    }

    @Test
    public void editUserDetailsByAdmin_InvalidRequest_ShouldReturnEditView() throws Exception {
        UUID adminId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();

        CustomUserDetails adminDetails = new CustomUserDetails(
                adminId,
                "admin@test.com",
                "encodedPassword",
                UserRole.ADMIN,
                RegistrationStatus.REGISTERED,
                UserStatus.ACTIVE
        );

        User adminUser = User.builder()
                .id(adminId)
                .email("admin@test.com")
                .reachedDegree(Degree.NONE)
                .role(UserRole.ADMIN)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .build();

        User targetUser = User.builder()
                .id(targetUserId)
                .email("user@test.com")
                .firstName("Ivan")
                .lastName("Ivanov")
                .reachedDegree(Degree.NONE)
                .role(UserRole.MEMBER)
                .isCompetitor(true)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .userPhone("0876989898")
                .contactPersonPhone("0877788778")
                .build();


        // Мокване на услугите
        when(userService.getUserById(adminId)).thenReturn(adminUser);
        when(userService.getUserById(targetUserId)).thenReturn(targetUser);

        MockHttpServletRequestBuilder request = post("/admin/users/details/edit/{id}", targetUserId)
                .with(user(adminDetails))
                .formField("userPhone","888")
                .formField("birthDate","1982-05-04")
                .formField("isCompetitor","true")
                .formField("reachedDegree", "NONE")
                .formField("ageGroup","M")
                .formField("height","180")
                .formField("weight","80")
                .formField("contactPerson","Georgi Ivanov")
                .formField("contactPersonPhone","55")
                .formField("role", "MEMBER") // Добавете всички полета от UserEditAdminRequest
                .formField("registrationStatus", "REGISTERED")
                .formField("status", "ACTIVE")
                .with(csrf());


        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("admin-user-edit"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("adminUser"))
                .andExpect(model().attributeExists("userEditAdminRequest"))
                .andExpect(model().attributeHasFieldErrors("userEditAdminRequest",
                        "contactPersonPhone","userPhone"));

        verify(userService, never()).editUserProfileByAdmin(any(), any());
    }


}