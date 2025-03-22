package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.service.EventService;
import cardindex.dojocardindex.EventParticipationRequest.model.EventParticipationRequest;
import cardindex.dojocardindex.EventParticipationRequest.service.EventParticipationService;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
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

    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @GetMapping("/pending")
    public ModelAndView getPendingRequests(@AuthenticationPrincipal CustomUserDetails details){

        User currentUser = userService.getUserById(details.getId());
        List<EventParticipationRequest> requests = requestService.getPendingRequests();
        List<User> requestUsersList = requests.stream().map(EventParticipationRequest::getUser).toList();
        Map<UUID, Integer>userAges = userService.getUserAges(requestUsersList);

        ModelAndView  modelAndView = new ModelAndView();
        modelAndView.setViewName("pending-requests");
        modelAndView.addObject("currentUser", currentUser);
        modelAndView.addObject("requests", requests);
        modelAndView.addObject("userAges",userAges);

        return modelAndView;

    }
    //TODO Да проверя constraints - за да реша дали има нужда да валидирам
    @PostMapping("/submit/{id}")
    public ModelAndView submitParticipationRequest(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails details){

        User user = userService.getUserById(details.getId());

        requestService.submitRequest(user.getId(),id);

        ModelAndView modelAndView = new ModelAndView("redirect:/events");
        modelAndView.addObject("user",user);

        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @PostMapping("/approve/{id}")
    public ModelAndView approveRequest(@PathVariable UUID id, @AuthenticationPrincipal CustomUserDetails details) {

        User adminUser = userService.getUserById(details.getId());

        requestService.approveRequest(id,adminUser);

        ModelAndView modelAndView = new ModelAndView("redirect:/events/requests/pending");
        modelAndView.addObject("adminUser", adminUser);

        return modelAndView;
    }

    @GetMapping("/{eventId}/event-details")
    public ModelAndView getEventDetails(@PathVariable UUID eventId,
                                        @AuthenticationPrincipal CustomUserDetails details) {

        Event event = eventService.getEventById(eventId);

        ModelAndView modelAndView = new ModelAndView("event-details");

        User currentUser = userService.getUserById(details.getId());
        modelAndView.addObject("currentUser", currentUser);
        modelAndView.addObject("event", event);
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @PutMapping("/{eventId}/users/{userId}/unapprove")
    public ModelAndView unApproveUserRequest(
            @PathVariable UUID eventId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails details) {

        Event event = eventService.getEventById(eventId);

        User currentUser = userService.getUserById(details.getId());

        User user = userService.getUserById(userId);

        requestService.unApproveRequest(event, user, currentUser);

        ModelAndView modelAndView = new ModelAndView("redirect:/events/" + eventId + "/details");
        modelAndView.addObject("successMessage", "Статусът на заявката е променен на 'REJECTED'.");
        modelAndView.addObject("currentUser", currentUser);

        return modelAndView;
    }
    //TODO Да добавя оправя двата метода за reject

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @GetMapping("/reject/{id}")
    public ModelAndView getRejectRequestPage(@PathVariable UUID id,
                                             @AuthenticationPrincipal CustomUserDetails details){

        User currentUser = userService.getUserById(details.getId());
        EventParticipationRequest request = requestService.getRequestById(id);



        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("request-reject");
        modelAndView.addObject("currentUser",currentUser);
        modelAndView.addObject("request",request);

        return modelAndView;

    }

    @PreAuthorize("hasAnyRole('ADMIN','TRAINER')")
    @PutMapping("/reject/{id}")
    public ModelAndView rejectUserRequest(@PathVariable UUID id,
                                          @RequestParam String reason,
                                          @AuthenticationPrincipal CustomUserDetails details){

        User currentUser = userService.getUserById(details.getId());
        EventParticipationRequest request = requestService.getRequestById(id);

        requestService.rejectRequest(id,currentUser,reason);

        ModelAndView modelAndView = new ModelAndView("redirect:/events/requests/pending");
        modelAndView.addObject("currentUser",currentUser);
        modelAndView.addObject("request",request);

        return modelAndView;

    }



}
