package cardindex.dojocardindex.EventParticipationRequest;

import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.EventParticipationRequest.model.EventParticipationRequest;
import cardindex.dojocardindex.EventParticipationRequest.model.RequestStatus;
import cardindex.dojocardindex.EventParticipationRequest.repository.EventParticipationRequestRepository;
import cardindex.dojocardindex.EventParticipationRequest.service.EventParticipationService;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventParticipationServiceUTest {

        @Mock
        private UserService userService;

        @Mock
        private EventParticipationRequestRepository requestRepository;

        @InjectMocks
        private EventParticipationService eventParticipationService;

        @Test
        void getRequestsBuUserId_ShouldReturnFilteredAndSortedRequests() {

            UUID userId = UUID.randomUUID();
            RequestStatus status = RequestStatus.PENDING;
            LocalDate today = LocalDate.now();

            User user = User.builder().id(userId).build();

            Event futureEvent = createEvent(today.plusDays(1));
            Event pastEvent = createEvent(today.minusDays(1));

            EventParticipationRequest validRequest = createRequest(user, futureEvent, status, today.minusDays(2).atStartOfDay());
            EventParticipationRequest wrongStatusRequest = createRequest(user, futureEvent, RequestStatus.APPROVED, today.minusDays(1).atStartOfDay());
            EventParticipationRequest pastEventRequest = createRequest(user, pastEvent, status, today.minusDays(3).atStartOfDay());

            when(userService.getUserById(userId)).thenReturn(user);
            when(requestRepository.findAllByUser(user))
                    .thenReturn(Arrays.asList(validRequest, wrongStatusRequest, pastEventRequest));

            List<EventParticipationRequest> result = eventParticipationService.getRequestsBuUserId(userId, status);

            //Трябва да върне само 1 заявка, която отговаря на условията
            assertEquals(1, result.size());
            //Върнатата заявка трябва да е валидната
            assertEquals(validRequest, result.get(0));
            verify(userService, times(1)).getUserById(userId);
            verify(requestRepository, times(1)).findAllByUser(user);
        }

        @Test
        void givenNonExistingUser_when_getRequestsBuUserId_then_ShouldReturnEmptyList() {

            UUID userId = UUID.randomUUID();
            RequestStatus status = RequestStatus.PENDING;

            when(userService.getUserById(userId)).thenReturn(null);


            List<EventParticipationRequest> result = eventParticipationService.getRequestsBuUserId(userId, status);

            // Трябва да върне празен списък за несъществуващ потребител
            assertTrue(result.isEmpty());
        }

        @Test
        void getRequestsBuUserId_ShouldReturnSortedRequestsInDescendingOrder() {

            UUID userId = UUID.randomUUID();
            RequestStatus status = RequestStatus.PENDING;
            LocalDate today = LocalDate.now();
            User user = User.builder().id(userId).build();


            Event event = createEvent(today.plusDays(1));

            EventParticipationRequest olderRequest = createRequest(user, event, status, today.minusDays(2).atStartOfDay());
            EventParticipationRequest newerRequest = createRequest(user, event, status, today.minusDays(1).atStartOfDay());

            when(userService.getUserById(userId)).thenReturn(user);
            when(requestRepository.findAllByUser(user))
                    .thenReturn(Arrays.asList(olderRequest, newerRequest));


            List<EventParticipationRequest> result = eventParticipationService.getRequestsBuUserId(userId, status);


            assertEquals(2, result.size());
            //"Заявките трябва да са сортирани в низходящ ред по дата на създаване"
            assertTrue(result.get(0).getCreated().isAfter(result.get(1).getCreated()));
        }


        private Event createEvent(LocalDate endDate) {

            return Event.builder()
                    .endDate(endDate).build();
        }

        private EventParticipationRequest createRequest(User user, Event event,
                                                        RequestStatus status, LocalDateTime created) {
            return EventParticipationRequest.builder()
            .user(user)
            .event(event)
            .status(status)
            .created(created)
            .build();

        }
}
