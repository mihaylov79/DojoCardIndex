package cardindex.dojocardindex.Event.service;


import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.models.EventType;
import cardindex.dojocardindex.Event.repository.EventRepository;
import cardindex.dojocardindex.User.models.Degree;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.exceptions.EventNotFoundException;
import cardindex.dojocardindex.exceptions.IllegalEventOperationException;
import cardindex.dojocardindex.web.dto.CreateEventRequest;
import cardindex.dojocardindex.web.dto.EditEventRequest;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

@Slf4j
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
                .result(false)
                .build();

        eventRepository.save(event);

    }

    public void closeEvent(UUID eventId){
        Event event = getEventById(eventId);

        event = event.toBuilder().closed(true).build();

        eventRepository.save(event);

    }

    public void showResultOnUpdateDetailsPage(UUID eventId){
        Event event = getEventById(eventId);

        if (event.getUsers().isEmpty()){

            throw new IllegalEventOperationException("Списъкът с участници за това събитие е празен. Няма резултати за визуализация.");
        }

        if (event.isResult()){
            throw new IllegalEventOperationException("Събитията вече са публикувани.");
        }

        event = event.toBuilder()
                .result(true)
                .build();

        eventRepository.save(event);
    }

    public void hideResultOnUpdateDetailsPage(UUID eventId){
        Event event = getEventById(eventId);

        if (!event.isResult()){
            throw new IllegalEventOperationException("Резултатите от това събитие все още не са публикувани.");
        }

        event = event.toBuilder()
                .result(false)
                .build();

        eventRepository.save(event);
    }

    //TODO Да добавя автоматично изпращане на мейл след обновявне на защитената степен!
    public void setExamResult(UUID eventId,UUID userId, Degree updatedDegree){

        Event event = getEventById(eventId);
        if (event.isResult()){
            throw new IllegalEventOperationException("Резултатите за това събитие вече са публикувани. " +
                    "Необходимо е да свалите резултатите преди да направите тази промяна.");
        }
        User user = userService.getUserById(userId);
        user = user.toBuilder()
                .reachedDegree(updatedDegree)
                .build();
        userService.saveUser(user);
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

        //Добавяме параметър place - който да използваме в updateWinner за по ясен код
        switch (place) {
            case 1 -> updateWinner(event, event.getFirstPlaceWinner(), user,
                    User::getAchievedFirstPlaces, User::setAchievedFirstPlaces, 1);
            case 2 -> updateWinner(event, event.getSecondPlaceWinner(), user,
                    User::getAchievedSecondPlaces, User::setAchievedSecondPlaces, 2);
            case 3 -> updateWinner(event, event.getThirdPlaceWinner(), user,
                    User::getAchievedThirdPlaces, User::setAchievedThirdPlaces, 3);
            default -> throw new IllegalArgumentException("Невалидна позиция. Позицията може да бъде 1, 2, или 3.");
        }


        eventRepository.save(event);
    }

    private void updateWinner(Event event, User oldWinner, User newWinner,
                              Function<User, Integer> getPlaceCount,
                              BiConsumer<User, Integer> setPlaceCount, int place) {

        if (oldWinner != null) {
            int newCount = Math.max(0, getPlaceCount.apply(oldWinner) - 1);
            setPlaceCount.accept(oldWinner, newCount);
        }

        if (newWinner != null) {
            setPlaceCount.accept(newWinner, getPlaceCount.apply(newWinner) + 1);
        }

        // Актуализираме съответната позиция въз основа на параметъра 'place' - place се добавя като параметър на метода
        if (place == 1) {
            event.setFirstPlaceWinner(newWinner);
        } else if (place == 2) {
            event.setSecondPlaceWinner(newWinner);
        } else if (place == 3) {
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
            int newCount = Math.max(0, getPlaceCount.apply(winner) - 1);
            setPlaceCount.accept(winner, newCount);
        }
    }


    public void saveEvent(Event event){
        eventRepository.save(event);
    }

//    public void exportEventDetailsAsCsv(UUID eventId, HttpServletResponse response) {
//
//        Event event = getEventById(eventId);
//        Set<User> users = event.getUsers();
//
//        response.setContentType("text/csv; charset=UTF-8");
//        response.setHeader("Content-Disposition","attachment; filename=event_" + eventId + ".csv");
//
//        try {
//        OutputStream out = response.getOutputStream();
//        out.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
//
//
//            PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
//
//            writer.println("Име,Фамилия,Дата на раждане,Категория,Тегло,Възраст,Степен,Мед. преглед");
//
//            users.stream().forEach(u -> writer.printf("%s,%s,%s,%s,%f,%d,%s,%s%n",
//                    u.getFirstName(),
//                    u.getLastName(),
//                    u.getBirthDate(),
//                    u.getAgeGroup(),
//                    u.getWeight(),
//                    userService.calculateAge(u.getBirthDate()),
//                    u.getReachedDegree(),
//                    u.getMedicalExamsPassed()));
//
//            writer.flush();
//            writer.close();
//
//        } catch (IOException e) {
//            log.error("Генерирането на CSV файл за събитие: {} беше неуспешно!", eventId, e);
//            throw new ExportIOException("Генерирането на PDF файл беще неуспешно!");
//        }
//
//    }
}
