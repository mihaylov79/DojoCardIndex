package cardindex.dojocardindex.Message;


import cardindex.dojocardindex.Message.Repository.MessageRepository;
import cardindex.dojocardindex.Message.Service.MessageService;
import cardindex.dojocardindex.Message.models.Message;
import cardindex.dojocardindex.User.models.Degree;
import cardindex.dojocardindex.User.models.UserRole;
import cardindex.dojocardindex.User.repository.UserRepository;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.web.dto.CreateUserRequest;
import cardindex.dojocardindex.web.dto.RegisterRequest;
import cardindex.dojocardindex.web.dto.SendMessageRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Collections;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
public class MessageITest {

   @Autowired
    private UserService userService;
   @Autowired
   private UserRepository userRepository;
   @Autowired
   private MessageService messageService;
    @Autowired
   private MessageRepository messageRepository;

    @Test
    void testSendMessageHappyPath(){

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

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user1@examplez.com",
                null,
                Collections.emptyList());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        SendMessageRequest messageRequest = new SendMessageRequest();
        messageRequest.setContent("Тестово съобщение!");
        messageRequest.setRecipient("admin@example.com");
        messageRequest.setCreated(LocalDateTime.now());
        messageRequest.setRead(false);

        messageService.sendMessage(messageRequest);
        //Проверява дали съобщението е създадено и запазено успешно
        assertEquals(1,messageRepository.findAll().size());

    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }


    @Test
    void testReplyMessageHappyPath(){

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

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user1@examplez.com",
                null,
                Collections.emptyList());


        Message message = Message.builder()
                .sender(userService.findUserByEmail("admin@example.com"))
                .recipient(userService.findUserByEmail("user1@examplez.com"))
                .content("Тестово съобщение")
                .isRead(false)
                .created(LocalDateTime.now())
                .build();

        messageRepository.save(message);


        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);



        messageService.replyMessage(message.getId(),"Отговор на Тест");

        //Проверява дали отговорът на съобщението е създадено и запазено успешно
        assertEquals(2,messageRepository.findAll().size());

        boolean replyExists = messageRepository.findAll().stream()
                .anyMatch(m -> m.getContent().equals("Отговор на Тест"));
        //Провеява дали е намерено съобщение със съдържание Отговор на Тест
        assertTrue(replyExists);

    }

    @Test
    void testRemoveMessageHappyPath(){

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

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "user1@examplez.com",
                null,
                Collections.emptyList());

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        Message message = Message.builder()
                .sender(userService.findUserByEmail("admin@example.com"))
                .recipient(userService.findUserByEmail("user1@examplez.com"))
                .content("Тестово съобщение")
                .isRead(false)
                .created(LocalDateTime.now())
                .build();

        messageRepository.save(message);

        assertEquals(1,messageRepository.findAll().size());

        messageService.removeMessage(message.getId());
        //Проверява дали статуса на съобщението е променен на read
        assertTrue( messageRepository.findById(message.getId()).get().isRead());


    }

}
