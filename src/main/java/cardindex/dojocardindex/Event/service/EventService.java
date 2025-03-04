package cardindex.dojocardindex.Event.service;


import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.repository.EventRepository;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.exceptions.EventNotFoundException;
import cardindex.dojocardindex.web.dto.CreateEventRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final UserService userService;


    @Autowired
    public EventService(EventRepository eventRepository, UserService userService) {
        this.eventRepository = eventRepository;
        this.userService = userService;

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
    //TODO Да добавя възможност за задаване на победителите в събитието или в едит или в отделен метод
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



    public Event getEventBuId(UUID eventId){
        return eventRepository.findById(eventId).orElseThrow(() ->new EventNotFoundException("Събитие с идентификация [%s] не съществува".formatted(eventId)));
    }

    public List<Event> getAllActiveEvents(){

        return eventRepository.findAllByClosedOrderByStartDate(false);

    }


}
