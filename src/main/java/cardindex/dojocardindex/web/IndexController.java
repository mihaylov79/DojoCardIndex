package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Event.service.EventService;
import cardindex.dojocardindex.Message.Service.MessageService;
import cardindex.dojocardindex.Message.models.Message;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.LoginRequest;
import cardindex.dojocardindex.web.dto.RegisterRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
public class IndexController {

    private final UserService userService;
    private final MessageService messageService;
    private final HttpSession httpSession;
    private final EventService eventService;

    @Autowired
    public IndexController(UserService userService, MessageService messageService, HttpSession httpSession, EventService eventService) {
        this.userService = userService;
        this.messageService = messageService;
        this.httpSession = httpSession;
        this.eventService = eventService;
    }

    @GetMapping("/")
    public String getIndexPage() {

        return "index";
    }

    @GetMapping("/register")
    public ModelAndView getRegistrationPage(){

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("register");
        modelAndView.addObject("registerRequest", new RegisterRequest());

        return modelAndView;
    }

    @GetMapping("/login")
    public ModelAndView getLoginPage(@RequestParam(value = "error",required = false) String errorParam){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        modelAndView.addObject("loginRequest", new LoginRequest());

        if(errorParam != null ) {
            modelAndView.addObject("errorMessage", "Грешно потбребитеско име или парола");
        }
        return modelAndView;
    }

    @PostMapping("/register")
    public ModelAndView registerUser(@Valid RegisterRequest registerRequest, BindingResult result){

        if (result.hasErrors()) {
            return new ModelAndView("register");
        }

        userService.register(registerRequest);

        return new ModelAndView("redirect:/login");

    }

    @GetMapping("/home")
    public ModelAndView getHomePage(@AuthenticationPrincipal CustomUserDetails details) {

        User user = userService.getUserById(details.getId());

        List<Message> unreadMessages = messageService.getReceivedMessagesByUser(user.getId());
        List<Event> events = eventService.getUpcomingEvents();

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("home-fixed");
        modelAndView.addObject("user", user);
        modelAndView.addObject("messages", unreadMessages);
        modelAndView.addObject("events",events);

        return modelAndView;
    }


}
