package cardindex.dojocardindex.EventParticipationRequest.service;

import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.models.EventType;
import cardindex.dojocardindex.Event.repository.EventRepository;
import cardindex.dojocardindex.Event.service.EventService;
import cardindex.dojocardindex.EventParticipationRequest.model.EventParticipationRequest;
import cardindex.dojocardindex.EventParticipationRequest.model.RequestStatus;
import cardindex.dojocardindex.EventParticipationRequest.repository.EventParticipationRequestRepository;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.exceptions.EventClosedException;
import cardindex.dojocardindex.exceptions.RequestNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;



@Service
public class EventParticipationService {

    private final EventParticipationRequestRepository requestRepository;
    private final UserService userService;
    private final EventService eventService;
    private final EventRepository eventRepository;

    @Autowired
    public EventParticipationService(EventParticipationRequestRepository requestRepository, UserService userService, EventService eventService, EventRepository eventRepository) {
        this.requestRepository = requestRepository;
        this.userService = userService;
        this.eventService = eventService;
        this.eventRepository = eventRepository;
    }

    public void submitRequest(UUID userId, UUID eventId){

        User user = userService.getUserById(userId);

        Event event = eventService.getEventById(eventId);

        if (event.isClosed()){
            throw new EventClosedException();
        }

        requestRepository.findByUserAndEvent(user,event).ifPresent(existingRequest ->{
            switch (existingRequest.getStatus()){
                case REJECTED -> throw new RuntimeException("Вашата заявка е била отхвърлена!");
                case PENDING ->  throw new RuntimeException("Вашата заявка все още чака одобрение");
                case APPROVED -> throw new RuntimeException("Вашата заявка вече е била одобрена");
            }
        });

        EventParticipationRequest request = EventParticipationRequest.builder()
                .user(user)
                .event(event)
                .status(RequestStatus.PENDING)
                .created(LocalDateTime.now())
                .build();

        requestRepository.save(request);
    }

    public List<EventParticipationRequest> getPendingRequests(){
        return requestRepository.findByStatusOrderByEvent(RequestStatus.PENDING);
    }

    public EventParticipationRequest getRequestById(UUID requestId){
        return requestRepository.findById(requestId).orElseThrow(()-> new RequestNotFoundException("Заявка за участие с идентификация [%s] не съществува.".formatted(requestId)));
    }

    public void approveRequest(UUID requestId, User currentUser){

        EventParticipationRequest request = getRequestById(requestId);
        Event event = request.getEvent();
        User user = request.getUser();

        event.getUsers().add(user);
        user.getEvents().add(event);

        request.getEvent().getUsers().add(request.getUser());

        request = request.toBuilder()
                .status(RequestStatus.APPROVED)
                .processedBy(currentUser)
                .build();

        requestRepository.save(request);
        eventService.saveEvent(event);
        userService.saveUser(user);

       event.getUsers().forEach(u -> System.out.println(u.getEmail()));



    }

    public void rejectRequest(UUID requestId, User currentUser){

        EventParticipationRequest request = getRequestById(requestId);

        request = request.toBuilder()
                .status(RequestStatus.REJECTED)
                .processedBy(currentUser)
                .build();

        requestRepository.save(request);

    }

    public void unApproveRequest(Event event, User user, User currentUser) {

        EventParticipationRequest request = requestRepository
                .findByUserAndEvent(user, event)
                .orElseThrow(() -> new IllegalArgumentException("Заявката не е намерена"));

        request = request.toBuilder()
                .status(RequestStatus.REJECTED)
                .processedBy(currentUser)
                .build();

        requestRepository.save(request);

        if (event.getType() == EventType.TOURNAMENT &&
                (user.equals(event.getFirstPlaceWinner()) ||
                        user.equals(event.getSecondPlaceWinner()) ||
                        user.equals(event.getThirdPlaceWinner()))) {
            eventService.resetWinners(event.getId());
        }

        System.out.println("Потребителите преди премахване: " + event.getUsers().size());
        event.getUsers().remove(user);
        System.out.println("Потребителите след премахване: " + event.getUsers().size());
        user.getEvents().remove(event);

        eventService.saveEvent(event);
        userService.saveUser(user);

    }


}
