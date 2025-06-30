package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Message.Service.MessageService;
import cardindex.dojocardindex.Message.models.Message;
import cardindex.dojocardindex.User.models.*;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.SendMessageRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@WebMvcTest(MessageController.class)
public class MessageControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private MessageService messageService;


    @Test
    public void testGetSendMessagePageWithAuthenticatedUser() throws Exception {

        UUID userId = UUID.randomUUID();

        CustomUserDetails userDetails = createTestUserDetails(userId);

        User mockUser = User.builder()
                .id(userDetails.getId())
                .email(userDetails.getEmail())
                .firstName("Ivan")
                .lastName("Ivanov")
                .role(userDetails.getRole())
                .registrationStatus(userDetails.getRegistrationStatus())
                .status(userDetails.getUserStatus())
                .reachedDegree(Degree.NONE)
                .build();


        when(userService.getUserById(userId)).thenReturn(mockUser);

        mockMvc.perform(get("/messages/send")
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("send-message"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("sendMessageRequest"));
    }

    @Test
    public void testSendMessage_Success() throws Exception {

        UUID userId = UUID.randomUUID();
        CustomUserDetails userDetails = createTestUserDetails(userId);

        User mockUser = User.builder()
                .id(userDetails.getId())
                .email(userDetails.getEmail())
                .firstName("Ivan")
                .lastName("Ivanov")
                .role(userDetails.getRole())
                .registrationStatus(userDetails.getRegistrationStatus())
                .status(userDetails.getUserStatus())
                .reachedDegree(Degree.NONE)
                .build();


        when(userService.getUserById(userId)).thenReturn(mockUser);

        mockMvc.perform(post("/messages/send")
                        .param("content", "Hello")
                        .param("recipient", mockUser.getEmail())
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/home"));

        verify(messageService).sendMessage(any(SendMessageRequest.class));
    }

    @Test
    public void testSendMessage_ValidationErrors() throws Exception {

        UUID userId = UUID.randomUUID();
        CustomUserDetails userDetails = createTestUserDetails(userId);

        User mockUser = User.builder()
                .id(userDetails.getId())
                .email(userDetails.getEmail())
                .firstName("Ivan")
                .lastName("Ivanov")
                .role(userDetails.getRole())
                .registrationStatus(userDetails.getRegistrationStatus())
                .status(userDetails.getUserStatus())
                .reachedDegree(Degree.NONE)
                .build();

        when(userService.getUserById(userId)).thenReturn(mockUser);

        mockMvc.perform(post("/messages/send")
                        .param("content", "")
                        .param("recipient", "")
                        .with(user(userDetails))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("send-message"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("sendMessageRequest"));

        verify(messageService, never()).sendMessage(any());
    }


    @Test
    public void testGetReplyPage_Success() throws Exception {

        UUID userId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        CustomUserDetails userDetails = createTestUserDetails(userId);

        User mockUser = User.builder()
                .id(userDetails.getId())
                .email(userDetails.getEmail())
                .firstName("Ivan")
                .lastName("Ivanov")
                .role(userDetails.getRole())
                .registrationStatus(userDetails.getRegistrationStatus())
                .status(userDetails.getUserStatus())
                .reachedDegree(Degree.NONE)
                .build();

        Message mockMessage = Message.builder()
                .id(messageId)
                .sender(mockUser)
                .recipient(mockUser)
                .content("Тест")
                .created(LocalDateTime.now())
                .isRead(false)
                .build();


        when(userService.getUserById(userId)).thenReturn(mockUser);
        when(messageService.findMessageById(messageId)).thenReturn(mockMessage);


        mockMvc.perform(get("/messages/reply/{id}", messageId)
                        .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("message-reply"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("message"));
    }


    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static CustomUserDetails createAdminUserDetails(UUID userId) {
        return new CustomUserDetails(
                userId,
                "admin@example.com",
                "password",
                UserRole.ADMIN,
                RegistrationStatus.REGISTERED,
                UserStatus.ACTIVE);

    }

    private static CustomUserDetails createTestUserDetails(UUID userId) {
        return new CustomUserDetails(
                userId,
                "user2@example.com",
                "password",
                UserRole.MEMBER,
                RegistrationStatus.REGISTERED,
                UserStatus.ACTIVE);
    }

}
