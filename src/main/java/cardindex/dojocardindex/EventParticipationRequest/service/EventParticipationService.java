package cardindex.dojocardindex.EventParticipationRequest.service;

import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.models.EventType;
import cardindex.dojocardindex.Event.service.EventService;
import cardindex.dojocardindex.EventParticipationRequest.model.EventParticipationRequest;
import cardindex.dojocardindex.EventParticipationRequest.model.RequestStatus;
import cardindex.dojocardindex.EventParticipationRequest.repository.EventParticipationRequestRepository;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.exceptions.EventClosedException;
import cardindex.dojocardindex.exceptions.RequestAlreadyExistException;
import cardindex.dojocardindex.exceptions.RequestNotFoundException;
import cardindex.dojocardindex.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;


@Service
public class EventParticipationService {

    private final EventParticipationRequestRepository requestRepository;
    private final UserService userService;
    private final EventService eventService;
    private final NotificationService notificationService;


    @Autowired
    public EventParticipationService(EventParticipationRequestRepository requestRepository, UserService userService, EventService eventService, NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.userService = userService;
        this.eventService = eventService;
        this.notificationService = notificationService;
    }

    public void submitRequest(UUID userId, UUID eventId){

        User user = userService.getUserById(userId);

        Event event = eventService.getEventById(eventId);

        if (event.isClosed()){
            throw new EventClosedException();
        }

        requestRepository.findByUserAndEvent(user,event).ifPresent(existingRequest ->{
            switch (existingRequest.getStatus()){
                case REJECTED -> throw new RequestAlreadyExistException("Вашата заявка е била отхвърлена!");
                case PENDING ->  throw new RequestAlreadyExistException("Вашата заявка все още чака одобрение");
                case APPROVED -> throw new RequestAlreadyExistException("Вашата заявка вече е била одобрена");
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

        String emailBody = "Вашата заявка за участие в %s - %s с начална дата: %s - беше одобрена. За повече информация посетете профилната си страница.".formatted(event.getEventDescription(),event.getLocation(),event.getStartDate());
        notificationService.sendNotification(user.getId(),user.getFirstName(), user.getLastName(), "Одобрена заявка за участие",emailBody);



       event.getUsers().forEach(u -> System.out.println(u.getEmail()));



    }

    public void rejectRequest(UUID requestId, User currentUser){

        EventParticipationRequest request = getRequestById(requestId);

        request = request.toBuilder()
                .status(RequestStatus.REJECTED)
                .processedBy(currentUser)
                .build();

        requestRepository.save(request);

        String emailBody = "Вашата заявка за участие в %s - %s с начална дата: %s - беше отхвърлена. За повече информация проверете меню Заявки на профилната си страница.".formatted(request.getEvent().getEventDescription(),request.getEvent().getLocation(),request.getEvent().getStartDate());
        notificationService.sendNotification(request.getUser().getId(),request.getUser().getFirstName(),request.getUser().getFirstName(),"Отхвърлена заявка за участие",emailBody);

    }

    public void rejectRequest(UUID requestId, User currentUser,String reason){

        EventParticipationRequest request = getRequestById(requestId);

        request = request.toBuilder()
                .status(RequestStatus.REJECTED)
                .reason(reason)
                .processedBy(currentUser)
                .build();

        requestRepository.save(request);

        String emailBody = "Вашата заявка за участие в %s - %s с начална дата: %s - беше отхвърлена. За повече информация проверете меню Заявки на профилната си страница.".formatted(request.getEvent().getEventDescription(),
                                                                                                                                                                      request.getEvent().getLocation(),
                                                                                                                                                                      request.getEvent().getStartDate());
        notificationService.sendNotification(request.getUser().getId(),request.getUser().getFirstName(),request.getUser().getFirstName(),"Отхвърлена заявка за участие",emailBody);
    }

    public void unApproveRequest(Event event, User user, User currentUser) {

        EventParticipationRequest request = requestRepository
                .findByUserAndEvent(user, event)
                .orElseThrow(() -> new IllegalArgumentException("Заявката не е намерена"));

        request = request.toBuilder()
                .status(RequestStatus.PENDING)
                .reason(null)
                .processedBy(currentUser)
                .build();

        requestRepository.save(request);

        if (event.getType() == EventType.TOURNAMENT &&
                (user.equals(event.getFirstPlaceWinner()) ||
                        user.equals(event.getSecondPlaceWinner()) ||
                        user.equals(event.getThirdPlaceWinner()))) {
            eventService.resetWinners(event.getId());
        }

        event.getUsers().remove(user);
        user.getEvents().remove(event);

        eventService.saveEvent(event);
        userService.saveUser(user);

        String emailBody = "Вашата заявка за участие в %s - %s с начална дата: %s беше върната за преразглеждане. Ще бъдете уведомени с мейл за по нататъшно развитие.".formatted(request.getEvent().getEventDescription(),request.getEvent().getLocation(),request.getEvent().getStartDate());
        notificationService.sendNotification(user.getId(),user.getFirstName(),user.getLastName(), "Върната заявка за участие",emailBody);

    }

    public List<EventParticipationRequest> getRequestsBuUserId(UUID userId,RequestStatus status){

        User currentUser = userService.getUserById(userId);

       return requestRepository.findAllByUser(currentUser).stream()
               .filter(r -> r.getEvent().getEndDate().isAfter(LocalDate.now())).filter(r->r.getStatus() == status)
               .sorted(Comparator.comparing(EventParticipationRequest::getCreated).reversed()).toList();
    }

    public List<EventParticipationRequest> getAllNotPendingRequests(){

       return requestRepository.findAllByStatusIsNot(RequestStatus.PENDING)
               .stream().filter(request->request.getEvent().getEndDate().isAfter(LocalDate.now()))
               .sorted(
                       Comparator.comparing(EventParticipationRequest::getCreated, Comparator.reverseOrder())
                               .thenComparing(r -> r.getEvent().getStartDate())
                               .thenComparing(EventParticipationRequest::getStatus)
               ).toList();
    }


}
