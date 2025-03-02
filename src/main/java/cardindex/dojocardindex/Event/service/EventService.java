package cardindex.dojocardindex.Event.service;


import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.repository.EventRepository;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.web.dto.CreateEventRequest;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserService userService;
    private final EmailValidator emailValidator;

    @Autowired
    public EventService(EventRepository eventRepository, UserService userService, EmailValidator emailValidator) {
        this.eventRepository = eventRepository;
        this.userService = userService;
        this.emailValidator = emailValidator;
    }

    public void addNewEvent(CreateEventRequest createEventRequest){

        Event event = Event.builder()
                .type(createEventRequest.getEventType())
                .EventDescription(createEventRequest.getEventDescription())
                .startDate(createEventRequest.getStartDate())
                .endDate(createEventRequest.getEndDate())
                .closed(false)
                .build();

        eventRepository.save(event);

    }

    public void closeEvent(UUID eventId){
        Event event = getEventBuId(eventId);

        event = event.toBuilder().closed(true).build();

        eventRepository.save(event);

    }

    public void editEvent(UUID eventId, CreateEventRequest createEventRequest){

        Event event = getEventBuId(eventId);

        event = event.toBuilder()
                .type(createEventRequest.getEventType())
                .EventDescription(createEventRequest.getEventDescription())
                .startDate(createEventRequest.getStartDate())
                .endDate(createEventRequest.getEndDate())
                .requirements(createEventRequest.getRequirements())
                .build();

        eventRepository.save(event);
    }


    //TODO да създам EventNotFoundException
    public Event getEventBuId(UUID eventId){
        return eventRepository.findById(eventId).orElseThrow(() ->new RuntimeException("Събитие с идентификация [%s] не съществува".formatted(eventId)));
    }


}
