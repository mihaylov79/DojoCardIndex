package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Event.service.EventService;
import cardindex.dojocardindex.EventParticipationRequest.service.EventParticipationService;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/events/requests")
public class EventParticipationRequestController {

    private final EventParticipationService requestService;
    private final UserService userService;
    private final EventService eventService;

    @Autowired
    public EventParticipationRequestController(EventParticipationService requestService, UserService userService, EventService eventService) {
        this.requestService = requestService;
        this.userService = userService;
        this.eventService = eventService;
    }

    @PostMapping("/approve/{id}")
    public ModelAndView approveRequest(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails details) {

        User adminUser = userService.getUserById(details.getId());

        requestService.approveRequest(id,adminUser);

        ModelAndView modelAndView = new ModelAndView("redirect:/events/requests/pending");
        modelAndView.addObject("adminUser", adminUser);

        return modelAndView;
    }

}
