package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.service.EventService;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.CreateEventRequest;
import cardindex.dojocardindex.web.mapper.DTOMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.naming.Binding;
import java.util.List;
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
}
