package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.models.EventType;
import cardindex.dojocardindex.Event.models.Requirements;
import cardindex.dojocardindex.Event.service.EventService;
import cardindex.dojocardindex.Message.Service.MessageService;
import cardindex.dojocardindex.Message.models.Message;
import cardindex.dojocardindex.User.models.RegistrationStatus;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.models.UserRole;
import cardindex.dojocardindex.User.models.UserStatus;
import cardindex.dojocardindex.User.repository.UserRepository;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//@SpringBootTest
//@AutoConfigureMockMvc
@WebMvcTest(IndexController.class)
public class IndexControllerApiTest {

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private MessageService messageService;
    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private UserRepository userRepository;


    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private DefaultErrorAttributes errorAttributes;

    @Test
    @WithMockUser
    void getRequestToIndexEndpoint_shouldReturnIndexView() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get("/");

        mockMvc.perform(requestBuilder).andExpect(status().isOk())
                                        .andExpect(view().name("index"));


    }

    @Test
    @WithMockUser
    void getRequestToRegisterEndpoint_shouldReturnRegisterView() throws Exception {

        MockHttpServletRequestBuilder request = get("/register");

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerRequest"));
    }


    @Test
    @WithMockUser
    void PostRequestToRegisterEndpointWithValidUser_registerUser_withValidData_shouldRedirectToLogin() throws Exception {

        User existingUser = getTestUser(UUID.randomUUID(),RegistrationStatus.NOT_REGISTERED,UserRole.MEMBER);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(existingUser));

        MockHttpServletRequestBuilder request = post("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "test@example.com")
                .param("firstName", "John")
                .param("lastName", "Doe")
                .param("password","123321")
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userService, times(1)).register(any());
    }

    @Test
    void PostRequestToRegisterEndpointWithErrors_registerUser_WithInvalidData_shouldRedirectRegister() throws Exception {

        User existingUser = getTestUser(UUID.randomUUID(),RegistrationStatus.NOT_REGISTERED,UserRole.MEMBER);

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(existingUser));

        MockHttpServletRequestBuilder request = post("http://localhost:8080/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "test@example.com")
                .param("firstName", "")
                .param("lastName", "")
                .param("password", "123456")
                .with(csrf()).with(user("anonymous"));

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        verify(userService, never()).register(any());
    }



    @Test
    void getHomePage_withAuthenticatedUser_shouldReturnHomePage() throws Exception {

        User mockUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .password("3332221")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.MEMBER)
                .status(UserStatus.ACTIVE)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(
                mockUser.getId(),
                mockUser.getEmail(),
                mockUser.getPassword(),
                mockUser.getRole(),
                mockUser.getRegistrationStatus(),
                mockUser.getStatus()
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails,"3332221",userDetails.getAuthorities()));

        User recipient = getTestUser(UUID.randomUUID(),RegistrationStatus.REGISTERED,UserRole.MEMBER);

        List<Message> unreadMessages = List.of(Message.builder()
                                                        .id(UUID.randomUUID())
                                                        .recipient(recipient)
                                                        .sender(mockUser)
                                                        .content("TestMessage")
                                                        .created(LocalDateTime.now())
                                                        .isRead(false).build());

        List<Event> upcomingEvents = List.of(Event.builder()
                                                    .id(UUID.randomUUID())
                                                    .type(EventType.TOURNAMENT)
                                                    .EventDescription("Test Event")
                                                    .startDate(LocalDate.now())
                                                    .endDate(LocalDate.now().plusDays(1))
                                                    .location("Sofia").requirements(Requirements.NONE)
                                                    .closed(false).build());

        MockHttpServletRequestBuilder request = get("/home").with(user(userDetails));

        when(userService.getUserById(any())).thenReturn(mockUser);
        when(messageService.getReceivedMessagesByUser(any())).thenReturn(unreadMessages);
        when(eventService.getUpcomingEvents()).thenReturn(upcomingEvents);


        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("home-fixed"))
                .andExpect(model().attribute("user", mockUser))
                .andExpect(model().attribute("messages", unreadMessages))
                .andExpect(model().attribute("events", upcomingEvents));
    }




    private static User getTestUser(UUID userId,RegistrationStatus registrationStatus,UserRole userRole) {
        return User.builder()
                .id(userId)
                .email("john@example.com")
                .password("2222313")
                .role(userRole)
                .status(UserStatus.ACTIVE)
                .registrationStatus(registrationStatus)
                .build();

    }


}
