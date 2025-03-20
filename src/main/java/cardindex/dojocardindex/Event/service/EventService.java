package cardindex.dojocardindex.Event.service;


import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.models.EventType;
import cardindex.dojocardindex.Event.repository.EventRepository;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.exceptions.EventNotFoundException;
import cardindex.dojocardindex.web.dto.CreateEventRequest;
import cardindex.dojocardindex.web.dto.EditEventRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

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
                .location(createEventRequest.getLocation())
                .requirements(createEventRequest.getRequirements())
                .closed(false)
                .build();

        eventRepository.save(event);

    }

    public void closeEvent(UUID eventId){
        Event event = getEventById(eventId);

        event = event.toBuilder().closed(true).build();

        eventRepository.save(event);

    }
    //TODO Да добавя възможност за задаване на победителите в събитието или в едит или в отделен метод
    public void editEvent(UUID eventId, EditEventRequest editEventRequest){

        Event event = getEventById(eventId);

        event = event.toBuilder()
                .type(editEventRequest.getEventType())
                .EventDescription(editEventRequest.getEventDescription())
                .startDate(editEventRequest.getStartDate())
                .location(editEventRequest.getLocation())
                .endDate(editEventRequest.getEndDate())
                .requirements(editEventRequest.getRequirements())
                .build();

        eventRepository.save(event);
    }



    public Event getEventById(UUID eventId){
        return eventRepository.findById(eventId).orElseThrow(() ->new EventNotFoundException("Събитие с идентификация [%s] не съществува".formatted(eventId)));
    }

    public List<Event> getAllActiveEvents(){

        return eventRepository.findAllByClosedOrderByStartDate(false);

    }

    public List<Event> getUpcomingEvents(){

        return eventRepository.findAllByStartDateAfterAndClosed(LocalDate.now(),false, Limit.of(3),Sort.by(Sort.Order.by("startDate")));
    }


    @Transactional
    public void setWinner(UUID eventId, UUID userId, int place) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Събитието не е открито"));

        if (event.getType() != EventType.TOURNAMENT) {
            throw new IllegalStateException("Победители могат да бъдат задавани само в ТУРНИР");
        }

        User user = userService.getUserById(userId);

        if (!event.getUsers().contains(user)) {
            throw new IllegalArgumentException("Победителят трябва да бъде участник в събитието!");
        }

        switch (place) {
            case 1 -> updateWinner(event, event.getFirstPlaceWinner(), user,
                    User::getAchievedFirstPlaces, User::setAchievedFirstPlaces);
            case 2 -> updateWinner(event, event.getSecondPlaceWinner(), user,
                    User::getAchievedSecondPlaces, User::setAchievedSecondPlaces);
            case 3 -> updateWinner(event, event.getThirdPlaceWinner(), user,
                    User::getAchievedThirdPlaces, User::setAchievedThirdPlaces);
            default -> throw new IllegalArgumentException("Невалидна позиция. Позицията може да бъде 1, 2, или 3.");
        }

        eventRepository.save(event);
    }

    private void updateWinner(Event event, User oldWinner, User newWinner,
                              Function<User, Integer> getPlaceCount,
                              BiConsumer<User, Integer> setPlaceCount) {

        if (oldWinner != null) {
            setPlaceCount.accept(oldWinner, getPlaceCount.apply(oldWinner) - 1);
        }

        if (newWinner != null) {
            setPlaceCount.accept(newWinner, getPlaceCount.apply(newWinner) + 1);
        }


        int placeCount = getPlaceCount.apply(newWinner);
        assert newWinner != null;
        if (placeCount == newWinner.getAchievedFirstPlaces()) {
            event.setFirstPlaceWinner(newWinner);
        } else if (placeCount == newWinner.getAchievedSecondPlaces()) {
            event.setSecondPlaceWinner(newWinner);
        } else if (placeCount == newWinner.getAchievedThirdPlaces()) {
            event.setThirdPlaceWinner(newWinner);
        }
    }

    @Transactional
    public void resetWinners(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Събитието не е открито"));

        if (event.getType() != EventType.TOURNAMENT) {
            throw new IllegalStateException("Победители могат да бъдат задавани само в ТУРНИР");
        }

        resetWinner(event, event.getFirstPlaceWinner(), User::getAchievedFirstPlaces, User::setAchievedFirstPlaces);
        resetWinner(event, event.getSecondPlaceWinner(), User::getAchievedSecondPlaces, User::setAchievedSecondPlaces);
        resetWinner(event, event.getThirdPlaceWinner(), User::getAchievedThirdPlaces, User::setAchievedThirdPlaces);

        event = event.toBuilder()
                .firstPlaceWinner(null)
                .secondPlaceWinner(null)
                .thirdPlaceWinner(null)
                .build();

        eventRepository.save(event);
    }

    private void resetWinner(Event event, User winner,
                             Function<User, Integer> getPlaceCount,
                             BiConsumer<User, Integer> setPlaceCount) {
        if (winner != null) {
            setPlaceCount.accept(winner, getPlaceCount.apply(winner) - 1);
        }
    }

    public void saveEvent(Event event){
        eventRepository.save(event);
    }
}
