package cardindex.dojocardindex.web;


import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.models.EventType;
import cardindex.dojocardindex.Event.models.Requirements;
import cardindex.dojocardindex.Event.service.EventService;
import cardindex.dojocardindex.EventParticipationRequest.model.EventParticipationRequest;
import cardindex.dojocardindex.EventParticipationRequest.model.RequestStatus;
import cardindex.dojocardindex.EventParticipationRequest.service.EventParticipationService;
import cardindex.dojocardindex.User.models.*;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@WebMvcTest(EventParticipationRequestController.class)
public class EventParticipationControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventParticipationService requestService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private EventService eventService;


    @Test
    public void getPendingRequests_ShouldReturnViewWithRequests() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        CustomUserDetails userDetails = createCustomUserDetails(userId);

        User mockUser =  User.builder()
                .id(userId)
                .email("admin@example.com")
                .role(UserRole.ADMIN)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .build();

        Event event = Event.builder()
                .id(UUID.randomUUID())
                .type(EventType.TOURNAMENT)
                .EventDescription("Тест Турнир")
                .startDate(LocalDate.parse("2025-04-04", DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .endDate(LocalDate.parse("2025-04-05", DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .requirements(Requirements.NONE)
                .closed(false)
                .build();

        EventParticipationRequest request1 = EventParticipationRequest.builder()
                .id(UUID.randomUUID())
                .status(RequestStatus.PENDING)
                .user(User.builder().id(UUID.randomUUID()).build())
                .event(event)
                .build();

        List<EventParticipationRequest> mockRequests = Collections.singletonList(request1);
        Map<UUID, Integer> mockUserAges = Map.of(request1.getUser().getId(), 25);

        when(userService.getUserById(userId)).thenReturn(mockUser);
        when(requestService.getPendingRequests()).thenReturn(mockRequests);
        when(userService.getUserAges(anyList())).thenReturn(mockUserAges);

        Authentication auth = new UsernamePasswordAuthenticationToken(

                userDetails,
                null,
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(auth);


        mockMvc.perform(get("/events/requests/pending")
                .with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("pending-requests"))
                .andExpect(model().attribute("currentUser", mockUser))
                .andExpect(model().attribute("requests", mockRequests))
                .andExpect(model().attribute("userAges", mockUserAges));
    }



    @Test
    public void approveRequest_ShouldWorkWithAdminRole() throws Exception {

        UUID requestId = UUID.randomUUID();
        CustomUserDetails adminDetails = createCustomUserDetails(UUID.randomUUID());

        User adminUser = User.builder()
                .id(adminDetails.getId())
                .email(adminDetails.getEmail())
                .role(adminDetails.getRole())
                .status(adminDetails.getUserStatus())
                .registrationStatus(adminDetails.getRegistrationStatus())
                .reachedDegree(Degree.NONE)
                .build();

        when(userService.getUserById(adminDetails.getId())).thenReturn(adminUser);
        doNothing().when(requestService).approveRequest(requestId, adminUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        adminDetails,
                        null,
                        adminDetails.getAuthorities()
                )
        );

        mockMvc.perform(post("/events/requests/approve/{id}", requestId)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/requests/pending"));

        verify(requestService).approveRequest(requestId, adminUser);
    }

    @Test
    public void unApproveUserRequest_ShouldUnapproveAndRedirect_WhenAdmin() throws Exception {
        // Arrange
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CustomUserDetails adminDetails = createCustomUserDetails(userId);

        User adminUser = User.builder()
                .id(adminDetails.getId())
                .registrationStatus(RegistrationStatus.REGISTERED)
                .status(UserStatus.ACTIVE)
                .reachedDegree(Degree.NONE)
                .role(UserRole.ADMIN)
                .build();

        User targetUser = User.builder()
                .id(userId)
                .status(UserStatus.ACTIVE)
                .registrationStatus(RegistrationStatus.REGISTERED)
                .role(UserRole.MEMBER)
                .reachedDegree(Degree.NONE)
                .build();

        Event event = Event.builder()
                .id(eventId)
                .type(EventType.TOURNAMENT)
                .EventDescription("Tестов турнир")
                .startDate(LocalDate.parse("2025-05-05",DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .endDate(LocalDate.parse("2025-05-06",DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .location("Каспичан")
                .closed(false)
                .build();

        when(userService.getUserById(adminDetails.getId())).thenReturn(adminUser);
        when(userService.getUserById(userId)).thenReturn(targetUser);
        when(eventService.getEventById(eventId)).thenReturn(event);
        doNothing().when(requestService).unApproveRequest(event, targetUser, adminUser);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        adminDetails,
                        null,
                        adminDetails.getAuthorities()
                )
        );

        mockMvc.perform(put("/events/requests/{eventId}/users/{userId}/unapprove", eventId, userId)
                        .header("Referer", "/events/" + eventId + "/details")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/" + eventId + "/details"));

        verify(requestService).unApproveRequest(event, targetUser, adminUser);
    }

    @Test
    public void getRejectRequestPage_ShouldReturnRejectPage_WhenAdmin() throws Exception {

        UUID requestId = UUID.randomUUID();
        CustomUserDetails adminDetails = createCustomUserDetails(UUID.randomUUID());

        User adminUser = User.builder()
                .id(adminDetails.getId())
                .email(adminDetails.getEmail())
                .role(adminDetails.getRole())
                .registrationStatus(adminDetails.getRegistrationStatus())
                .status(adminDetails.getUserStatus())
                .reachedDegree(Degree.NONE)
                .build();

        Event event = Event.builder()
                .id(UUID.randomUUID())
                .type(EventType.TOURNAMENT)
                .EventDescription("Tестов турнир")
                .startDate(LocalDate.parse("2025-05-05",DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .endDate(LocalDate.parse("2025-05-06",DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .location("Каспичан")
                .closed(false)
                .build();


        EventParticipationRequest request = EventParticipationRequest.builder()
                .id(requestId)
                .status(RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .event(event)
                .user(User.builder().id(UUID.randomUUID()).build())
                .build();

        when(userService.getUserById(adminDetails.getId())).thenReturn(adminUser);
        when(requestService.getRequestById(requestId)).thenReturn(request);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        adminDetails,
                        null,
                        adminDetails.getAuthorities()
                )
        );

        mockMvc.perform(get("/events/requests/reject/{id}", requestId).with(user(adminDetails)))
                .andExpect(status().isOk())
                .andExpect(view().name("request-reject"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("request"))
                .andExpect(model().attribute("request", request));
    }

    @Test
    public void getRejectRequestPage_ShouldReturnUnAuthorized_WhenNotAuthenticated() throws Exception {

        UUID requestId = UUID.randomUUID();

        mockMvc.perform(get("/events/requests/reject/{id}", requestId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void rejectUserRequest_ShouldRejectAndRedirect_WhenAdmin() throws Exception {

        UUID requestId = UUID.randomUUID();
        String rejectionReason = "Тест";

        CustomUserDetails adminDetails = createCustomUserDetails(UUID.randomUUID());

        User adminUser = User.builder()
                .id(adminDetails.getId())
                .email(adminDetails.getEmail())
                .role(adminDetails.getRole())
                .registrationStatus(adminDetails.getRegistrationStatus())
                .status(adminDetails.getUserStatus())
                .reachedDegree(Degree.NONE)
                .build();

        Event event = Event.builder()
                .id(UUID.randomUUID())
                .type(EventType.TOURNAMENT)
                .EventDescription("Tестов турнир")
                .startDate(LocalDate.parse("2025-05-05",DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .endDate(LocalDate.parse("2025-05-06",DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .location("Каспичан")
                .closed(false)
                .build();


        EventParticipationRequest request = EventParticipationRequest.builder()
                .id(requestId)
                .status(RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .event(event)
                .user(User.builder().id(UUID.randomUUID()).build())
                .build();

        when(userService.getUserById(any())).thenReturn(adminUser);
        when(requestService.getRequestById(requestId)).thenReturn(request);
        doNothing().when(requestService).rejectRequest(requestId, adminUser, rejectionReason);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        adminDetails,
                        null,
                        adminDetails.getAuthorities()
                )
        );

        mockMvc.perform(put("/events/requests/reject/{id}", requestId)
                        .param("reason", rejectionReason)
                        .with(user(adminDetails))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/events/requests/pending"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attributeExists("request"));

        verify(requestService).rejectRequest(requestId, adminUser, rejectionReason);
    }

    @Test
    public void rejectUserRequest_ShouldReturnUnauthorized_WhenNotAuthenticated() throws Exception {
        
        UUID requestId = UUID.randomUUID();

        mockMvc.perform(put("/events/requests/reject/{id}", requestId)
                        .param("reason", "any reason")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getNotPendingRequestsPage_ShouldReturnViewWithRequests_WhenAdmin() throws Exception {

        UUID userId = UUID.randomUUID();
        CustomUserDetails adminDetails = createCustomUserDetails(userId);

        User mockUser = User.builder()
                .id(adminDetails.getId())
                .email(adminDetails.getEmail())
                .role(adminDetails.getRole())
                .registrationStatus(adminDetails.getRegistrationStatus())
                .status(adminDetails.getUserStatus())
                .reachedDegree(Degree.NONE)
                .build();

        Event event = Event.builder()
                .id(UUID.randomUUID())
                .type(EventType.TOURNAMENT)
                .EventDescription("Tестов турнир")
                .startDate(LocalDate.parse("2025-05-05",DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .endDate(LocalDate.parse("2025-05-06",DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .requirements(Requirements.NONE)
                .location("Каспичан")
                .closed(false)
                .build();

        User requestUser1 = User.builder()
                .id(UUID.randomUUID())
                .birthDate(LocalDate.now().minusYears(25))
                .build();

        User requestUser2 = User.builder()
                .id(UUID.randomUUID())
                .birthDate(LocalDate.now().minusYears(30))
                .build();

        EventParticipationRequest request1 = EventParticipationRequest.builder()
                .id(UUID.randomUUID())
                .status(RequestStatus.APPROVED)
                .user(requestUser1)
                .processedBy(mockUser)
                .event(event)
                .build();

        EventParticipationRequest request2 = EventParticipationRequest.builder()
                .id(UUID.randomUUID())
                .status(RequestStatus.REJECTED)
                .user(requestUser2)
                .processedBy(mockUser)
                .event(event)
                .build();

        List<EventParticipationRequest> mockRequests = Arrays.asList(request1, request2);
        Map<UUID, Integer> mockUserAges = Map.of(
                requestUser1.getId(), 25,
                requestUser2.getId(), 30
        );

        when(userService.getUserById(userId)).thenReturn(mockUser);
        when(requestService.getAllNotPendingRequests()).thenReturn(mockRequests);
        when(userService.getUserAges(anyList())).thenReturn(mockUserAges);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        adminDetails,
                        null,
                        adminDetails.getAuthorities()
                )
        );


        mockMvc.perform(get("/events/requests/not-pending"))
                .andExpect(status().isOk())
                .andExpect(view().name("not-pending"))
                .andExpect(model().attributeExists("notPendingRequests"))
                .andExpect(model().attributeExists("userAges"))
                .andExpect(model().attributeExists("currentUser"))
                .andExpect(model().attribute("notPendingRequests", mockRequests))
                .andExpect(model().attribute("userAges", mockUserAges));
    }




    private static CustomUserDetails createCustomUserDetails(UUID userId) {
        return new CustomUserDetails(
                userId,
                "admin@example.com",
                "password",
                UserRole.ADMIN,
                RegistrationStatus.REGISTERED,
                UserStatus.ACTIVE);

    }

}
