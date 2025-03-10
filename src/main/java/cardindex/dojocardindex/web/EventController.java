package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.service.EventService;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.CreateEventRequest;
import cardindex.dojocardindex.web.dto.EditEventRequest;
import cardindex.dojocardindex.web.mapper.DTOMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;


import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping ("/events")
public class EventController {

    private final EventService eventService;
    private final UserService userService;

    @Autowired
    public EventController(EventService eventService, UserService userService) {
        this.eventService = eventService;
        this.userService = userService;
    }

    @GetMapping
    public ModelAndView getEventsPage(@AuthenticationPrincipal CustomUserDetails details){

        User currentUser = userService.getUserById(details.getId());
        List<Event> events = eventService.getAllActiveEvents();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("events");
        modelAndView.addObject("currentUser", currentUser);
        modelAndView.addObject("events",events);
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @GetMapping("/new")
    public ModelAndView getCreateEventPage(@AuthenticationPrincipal CustomUserDetails details){

        User currentUser = userService.getUserById(details.getId());

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("new-event");
        modelAndView.addObject("currentUser",currentUser);
        modelAndView.addObject("createEventRequest", new CreateEventRequest());

        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @PostMapping("/new")
    public ModelAndView createNewEvent(@AuthenticationPrincipal CustomUserDetails details, CreateEventRequest createEventRequest, BindingResult result){

        User currentUser = userService.getUserById(details.getId());

        if (result.hasErrors()){
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("new-event");
            modelAndView.addObject("currentuser", currentUser);
            return modelAndView;
        }
        eventService.addNewEvent(createEventRequest);

        ModelAndView modelAndView = new ModelAndView("redirect:/events");
        modelAndView.addObject(currentUser);
        return modelAndView;
    }

    @PutMapping("/{eventId}/close")
    public ModelAndView closeEvent(@PathVariable UUID eventId, @AuthenticationPrincipal CustomUserDetails details){

        User currentuser = userService.getUserById(details.getId());

        eventService.closeEvent(eventId);

        ModelAndView modelAndView = new ModelAndView("redirect:/events");
        modelAndView.addObject("currentuser", currentuser);
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @GetMapping("/edit/{id}")
    public ModelAndView getEditEventPage(@PathVariable UUID id,@AuthenticationPrincipal CustomUserDetails details){

        User currentUser = userService.getUserById(details.getId());
        Event event = eventService.getEventById(id);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("edit-event");
        modelAndView.addObject("currentUser", currentUser);
        modelAndView.addObject("event",event);
        modelAndView.addObject("editEventRequest", DTOMapper.mapEventToEditEventRequest(event));

        return modelAndView;

    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @PutMapping("/edit/{id}")
    public ModelAndView editEvent(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails details, @Valid EditEventRequest editEventRequest, BindingResult result){

        User currentUser = userService.getUserById(details.getId());
        Event event = eventService.getEventById(id);

        if (result.hasErrors()){
            ModelAndView modelAndView = new ModelAndView();
            modelAndView.setViewName("edit-event");
            modelAndView.addObject("currentUser",currentUser);
            modelAndView.addObject("event",event);
            modelAndView.addObject("editEventRequest",editEventRequest);
            return modelAndView;
        }

        eventService.editEvent(id,editEventRequest);
        ModelAndView modelAndView = new ModelAndView("redirect:/events");
        modelAndView.addObject(currentUser);

        return modelAndView;

    }
    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @GetMapping("/{eventId}/winners")
    public ModelAndView showSetWinnersPage(@PathVariable UUID eventId) {

        Event event = eventService.getEventById(eventId);

        System.out.println("Участници в събитието:");
        event.getUsers().forEach(user -> System.out.println(user.getEmail())); // Дебъгване


        ModelAndView modelAndView = new ModelAndView("set-winners");
        modelAndView.addObject("event", event);
        modelAndView.addObject("users", event.getUsers()); // Всички участници в турнира
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @PostMapping("/{eventId}/set-winner")
    public ModelAndView setWinner(@PathVariable UUID eventId,
                                  @RequestParam UUID userId,
                                  @RequestParam int place) {

        eventService.setWinner(eventId, userId, place);

        return new ModelAndView("redirect:/events/" + eventId + "/winners");
    }

    @GetMapping("/{eventId}/details")
    public ModelAndView getEventDetails(@PathVariable UUID eventId,
                                        @AuthenticationPrincipal CustomUserDetails details) {

        Event event = eventService.getEventById(eventId);

        List<User> eventUsers = event.getUsers().stream().toList();
        Map<UUID, Integer> userAges = userService.getUserAges(eventUsers);

        ModelAndView modelAndView = new ModelAndView("event-details");

        User currentUser = userService.getUserById(details.getId());
        modelAndView.addObject("currentUser", currentUser);
        modelAndView.addObject("event", event);
        modelAndView.addObject("userAges",userAges);
        modelAndView.addObject("successMessage", "Победителите са нулирани успешно!");
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @PostMapping("/{eventId}/reset-winners")
    public ModelAndView resetWinners(@PathVariable UUID eventId, @AuthenticationPrincipal CustomUserDetails details) {

        User currentUser = userService.getUserById(details.getId());
        eventService.resetWinners(eventId);

        ModelAndView modelAndView = new ModelAndView("redirect:/events/{eventId}/details");
        modelAndView.addObject("currentUser", currentUser);
        modelAndView.addObject("successMessage", "Победителите са нулирани успешно!");

        return modelAndView;
    }

}
