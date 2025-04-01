package cardindex.dojocardindex.EventParticipationRequest;


import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.models.EventType;
import cardindex.dojocardindex.Event.models.Requirements;
import cardindex.dojocardindex.Event.repository.EventRepository;
import cardindex.dojocardindex.Event.service.EventService;
import cardindex.dojocardindex.EventParticipationRequest.model.EventParticipationRequest;
import cardindex.dojocardindex.EventParticipationRequest.model.RequestStatus;
import cardindex.dojocardindex.EventParticipationRequest.repository.EventParticipationRequestRepository;
import cardindex.dojocardindex.EventParticipationRequest.service.EventParticipationService;
import cardindex.dojocardindex.User.models.Degree;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.models.UserRole;
import cardindex.dojocardindex.User.repository.UserRepository;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.notification.service.NotificationService;
import cardindex.dojocardindex.web.dto.CreateUserRequest;
import cardindex.dojocardindex.web.dto.RegisterRequest;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashSet;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class EventParticipationRequestITest {

    @Autowired
    private EventParticipationRequestRepository requestRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventService eventService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private EventParticipationService participationService;



    @Test
    void testSubmitRequestHappyPath() {

        createTestUserWithSecurityContext();

        Event event = Event.builder()
                .type(EventType.TOURNAMENT)
                .EventDescription("Тест Турнир")
                .startDate(LocalDate.parse("05-05-2025",DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                .endDate(LocalDate.parse("06-05-2025",DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                .location("Каспичан")
                .requirements(Requirements.NONE)
                .closed(false)
                .build();

        eventRepository.save(event);

        participationService.submitRequest(userRepository.getByEmail("user1@examplez.com").getId(),event.getId());

        assertEquals(1,requestRepository.findAll().size());
    }

    @Test
    @Transactional
    void testApproveRequestHappyPath(){

        createTestUserWithSecurityContext();

        Event event = Event.builder()
                .type(EventType.TOURNAMENT)
                .EventDescription("Тест Турнир")
                .startDate(LocalDate.parse("05-05-2025",DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                .endDate(LocalDate.parse("06-05-2025",DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                .location("Каспичан")
                .requirements(Requirements.NONE)
                .users(new LinkedHashSet<>())
                .closed(false)
                .build();

        eventRepository.save(event);

        EventParticipationRequest request = EventParticipationRequest.builder()
                .event(event)
                .user(userRepository.getByEmail("user1@examplez.com"))
                .status(RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .build();

        requestRepository.save(request);

        User user = userRepository.getByEmail("user1@examplez.com");

        user = user.toBuilder()
                .events(new LinkedHashSet<>())
                .build();
        userRepository.save(user);


        participationService.approveRequest(request.getId(),user);

        Event updatedEvent = eventRepository.findById(event.getId()).orElseThrow();
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        EventParticipationRequest updatedRequest = requestRepository.findById(request.getId()).orElseThrow();

        assertEquals(RequestStatus.APPROVED,updatedRequest.getStatus());
        assertTrue(updatedEvent.getUsers().contains(updatedUser));
        assertTrue(updatedUser.getEvents().contains(updatedEvent));


    }

    @Test
    void testRejectRequest_happyPath(){

        createTestUserWithSecurityContext();

        Event event = Event.builder()
                .type(EventType.TOURNAMENT)
                .EventDescription("Тест Турнир")
                .startDate(LocalDate.parse("05-05-2025",DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                .endDate(LocalDate.parse("06-05-2025",DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                .location("Каспичан")
                .requirements(Requirements.NONE)
                .users(new LinkedHashSet<>())
                .closed(false)
                .build();

        eventRepository.save(event);


        participationService.submitRequest(userService.findUserByEmail("user1@examplez.com").getId(),event.getId());

        EventParticipationRequest request = requestRepository.findByUserAndEvent(userService.findUserByEmail("user1@examplez.com"),event).get();

        participationService.rejectRequest(request.getId(),
                                           userService.findUserByEmail("user1@examplez.com"),"Проба" );

        EventParticipationRequest updatedRequest = requestRepository.findById(request.getId()).orElseThrow();

        assertEquals(RequestStatus.REJECTED, updatedRequest.getStatus());

    }

    @Test
    void test_unApprovedRequest_happyPath(){

        createTestUserWithSecurityContext();

        Event event = Event.builder()
                .type(EventType.TOURNAMENT)
                .EventDescription("Тест Турнир")
                .startDate(LocalDate.parse("05-05-2025",DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                .endDate(LocalDate.parse("06-05-2025",DateTimeFormatter.ofPattern("dd-MM-yyyy")))
                .location("Каспичан")
                .requirements(Requirements.NONE)
                .users(new LinkedHashSet<>())
                .closed(false)
                .build();

        eventRepository.save(event);

        EventParticipationRequest request = EventParticipationRequest.builder()
                .event(event)
                .user(userRepository.getByEmail("user1@examplez.com"))
                .status(RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .build();

        requestRepository.save(request);

        User user = userRepository.getByEmail("user1@examplez.com");

        user = user.toBuilder()
                .events(new LinkedHashSet<>())
                .build();
        userRepository.save(user);


        participationService.approveRequest(request.getId(),user);


        participationService.unApproveRequest(event,user,user);

        Event updatedEvent = eventRepository.findById(event.getId()).orElseThrow();
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        EventParticipationRequest updatedRequest = requestRepository.findById(request.getId()).orElseThrow();

        assertSame(RequestStatus.PENDING, updatedRequest.getStatus());
        assertNull(updatedRequest.getReason());
       assertEquals(user,updatedRequest.getUser());
        assertEquals(user,updatedRequest.getProcessedBy());
        assertTrue(updatedEvent.getUsers().isEmpty());
        assertTrue(updatedUser.getEvents().isEmpty());

    }



    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void createTestUserWithSecurityContext() {
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
    }
}
