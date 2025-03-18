package cardindex.dojocardindex.web;


import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.notification.client.dto.Notification;
import cardindex.dojocardindex.notification.client.dto.NotificationPreference;
import cardindex.dojocardindex.notification.service.NotificationService;
import cardindex.dojocardindex.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final UserService userService;
    private final NotificationService notificationService;

    @Autowired
    public NotificationController(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public ModelAndView getNotificationsPage(@AuthenticationPrincipal CustomUserDetails details){

        User user = userService.getUserById(details.getId());

        NotificationPreference notificationPreference = notificationService.getUserNotificationPreference(details.getId());
        List<Notification> notificationHistory = notificationService.getNotificationHistory(details.getId());

        ModelAndView modelAndView = new ModelAndView("notifications");
        modelAndView.addObject("user", user);
        modelAndView.addObject("notificationPreference",notificationPreference);
        modelAndView.addObject("notificationHistory", notificationHistory);

        return modelAndView;

    }


}
