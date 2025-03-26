package cardindex.dojocardindex.Event;

import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.models.EventType;
import cardindex.dojocardindex.Event.models.Requirements;
import cardindex.dojocardindex.Event.repository.EventRepository;
import cardindex.dojocardindex.Event.service.EventService;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.exceptions.EventNotFoundException;
import cardindex.dojocardindex.web.dto.CreateEventRequest;
import cardindex.dojocardindex.web.dto.EditEventRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class EventServiceUTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private EventService eventService;


    @Test
    void given_nonExistingEvent_when_getById_then_throwEventNotFoundException(){

        UUID eventId = UUID.randomUUID();

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class,()->eventService.getEventById(eventId));
    }

    @Test
    void given_existingOpenEvent_when_closeEvent_them_closedFieldIsSetToTrueAndSaveEventToDb(){

        UUID eventId = UUID.randomUUID();
        Event testEvent = Event.builder()
                                .id(eventId)
                                .closed(false)
                                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(testEvent));

        eventService.closeEvent(eventId);

        verify(eventRepository,times(1))
                                    .save(Mockito.argThat(Event::isClosed));
    }

    @Test
    void given_CreateEventRequest_when_addNewEvent_then_newEventIsCreatedAndSaveToDb(){

        CreateEventRequest dto = CreateEventRequest.builder()
                .eventType(EventType.TOURNAMENT)
                .eventDescription("Турнир")
                .startDate(LocalDate.parse("2025-04-01"))
                .endDate(LocalDate.parse("2025-04-02"))
                .location("Ловеч")
                .requirements(Requirements.NONE)
                .build();

        eventService.addNewEvent(dto);
        verify(eventRepository, times(1)).save(Mockito.argThat(savedEvent ->
                savedEvent.getType().equals(dto.getEventType()) &&
                savedEvent.getEventDescription().equals(dto.getEventDescription()) &&
                savedEvent.getStartDate().equals(dto.getStartDate()) &&
                savedEvent.getEndDate().equals(dto.getEndDate()) &&
                savedEvent.getLocation().equals(dto.getLocation()) &&
                savedEvent.getRequirements().equals(dto.getRequirements()) &&
                !savedEvent.isClosed()
                ));

    }

    @Test
    void given_nonExistingEvent_when_editEvent_ThrowEventNotFoundException(){

        UUID eventId = UUID.randomUUID();

        EditEventRequest dto = EditEventRequest.builder()
                .eventType(EventType.TOURNAMENT)
                .eventDescription("Турнир")
                .startDate(LocalDate.parse("2025-04-01"))
                .endDate(LocalDate.parse("2025-04-02"))
                .location("Ловеч")
                .requirements(Requirements.NONE)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, ()-> eventService.editEvent(eventId, dto));

    }

    @Test
    void given_ExistingEvent_when_editEvent_then_changeDetailsAndSaveDb(){

        UUID eventId = UUID.randomUUID();

       Event event = Event.builder()
               .id(eventId)
               .type(EventType.SEMINAR)
               .EventDescription("Семинар")
               .startDate(LocalDate.parse("2025-04-10"))
               .endDate(LocalDate.parse("2025-04-11"))
               .location("София")
               .requirements(Requirements.KYU_5)
               .closed(false)
               .build();

        EditEventRequest dto = EditEventRequest.builder()
                .eventType(EventType.TOURNAMENT)
                .eventDescription("Турнир")
                .startDate(LocalDate.parse("2025-04-01"))
                .endDate(LocalDate.parse("2025-04-02"))
                .location("Ловеч")
                .requirements(Requirements.NONE)
                .build();

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        eventService.editEvent(eventId,dto);

        verify(eventRepository,times(1)).save(Mockito.argThat(savedEvent ->
                savedEvent.getType().equals(dto.getEventType()) &&
                savedEvent.getEventDescription().equals(dto.getEventDescription()) &&
                savedEvent.getStartDate().equals(dto.getStartDate()) &&
                savedEvent.getEndDate().equals(dto.getEndDate()) &&
                savedEvent.getLocation().equals(dto.getLocation()) &&
                savedEvent.getRequirements().equals(dto.getRequirements())
                ));

    }



}
