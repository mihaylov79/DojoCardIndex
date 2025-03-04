package cardindex.dojocardindex.EventParticipationRequest.service;

import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.service.EventService;
import cardindex.dojocardindex.EventParticipationRequest.model.EventParticipationRequest;
import cardindex.dojocardindex.EventParticipationRequest.model.RequestStatus;
import cardindex.dojocardindex.EventParticipationRequest.repository.EventParticipationRequestRepository;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
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

    @Autowired
    public EventParticipationService(EventParticipationRequestRepository requestRepository, UserService userService, EventService eventService) {
        this.requestRepository = requestRepository;
        this.userService = userService;
        this.eventService = eventService;
    }

    public void submitRequest(UUID userId, UUID eventId){

        User user = userService.getUserById(userId);

        Event event = eventService.getEventById(eventId);

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

    public void approveRequest(UUID requestId){

        EventParticipationRequest request = getRequestById(requestId);

        request.getEvent().getUsers().add(request.getUser());
        request = request.toBuilder().status(RequestStatus.APPROVED).build();

        requestRepository.save(request);
    }

    public void rejectRequest(UUID requestId){

        EventParticipationRequest request = getRequestById(requestId);

        request = request.toBuilder().status(RequestStatus.REJECTED).build();

        requestRepository.save(request);

    }



}
