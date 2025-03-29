package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Event.service.EventService;
import cardindex.dojocardindex.Message.Service.MessageService;
import cardindex.dojocardindex.User.models.RegistrationStatus;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.models.UserStatus;
import cardindex.dojocardindex.User.repository.UserRepository;
import cardindex.dojocardindex.User.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


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

//    @Test
//    @WithMockUser
//    void getRequestToRegisterEndpoint_shouldReturnRegisterView() throws Exception {
//
//        MockHttpServletRequestBuilder request = get("/register");
//
//        mockMvc.perform(request)
//                .andExpect(status().isOk())
//                .andExpect(view().name("register"))
//                .andExpect(model().attributeExists("registerRequest"));
//    }

//    @Test
//    void getRequestToLoginEndpoint_shouldReturnLoginView() throws Exception {
//
//        MockHttpServletRequestBuilder request = get("/login");
//
//        mockMvc.perform(request)
//                .andExpect(status().isOk())
//                .andExpect(view().name("login"))
//                .andExpect(model().attributeExists("loginRequest"));
//    }

//    @Test
//    @WithMockUser
//    void getRequestToLoginEndpointWithParameter_shouldReturnLoginViewAndErrorAttribute() throws Exception {
//
//        MockHttpServletRequestBuilder request = get("/login").param("error","");
//
//        mockMvc.perform(request)
//                .andExpect(status().isOk())
//                .andExpect(view().name("login"))
//                .andExpect(model().attributeExists("loginRequest","errorMessage"));
//    }

//    @Test
//    void postRequestToLoginEndpointWithParameter_shouldReturnLoginViewAndErrorAttribute() throws Exception {
//
//        MockHttpServletRequestBuilder request = post("/login")
//                .formField("firstName","Iva")
//                .formField("lastName","Ivanovа")
//                .formField("email","iva@home.bg")
//                .formField("password",(new BCryptPasswordEncoder().encode("123321"))).with(csrf());
//
//        mockMvc.perform(request)
//                .andExpect(status().is3xxRedirection())
//                .andExpect(redirectedUrl("/login"));
//        verify(userService,times(1)).register(any());
//    }

    @Test
    @WithMockUser
    void PostRequestToRegisterEndpointWithValidUser_registerUser_withValidData_shouldRedirectToLogin() throws Exception {

        User existingUser = User.builder()
                .email("test@example.com")
                .status(UserStatus.ACTIVE)
                .registrationStatus(RegistrationStatus.NOT_REGISTERED)
                .build();

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(existingUser));

        MockHttpServletRequestBuilder request = post("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "test@example.com")
                .param("firstName", "John")
                .param("lastName", "Doe")
                .param("password", "123456")
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userService, times(1)).register(any());
    }

    @Test
    @WithMockUser
    void PostRequestToRegisterEndpointWithErrors_registerUser_WithInvalidDara_shouldRedirectRegister() throws Exception {

        User existingUser = User.builder()
                .email("test@example.com")
                .status(UserStatus.ACTIVE)
                .registrationStatus(RegistrationStatus.NOT_REGISTERED)
                .build();

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(existingUser));

        MockHttpServletRequestBuilder request = post("/register")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "test@example.com")
                .param("firstName", "")
                .param("lastName", "")
                .param("password", "123456")
                .with(csrf());

        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        verify(userService, never()).register(any());
    }


}
