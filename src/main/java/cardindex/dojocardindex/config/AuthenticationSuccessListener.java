package cardindex.dojocardindex.config;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.notification.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationSuccessListener {

    private final NotificationService notificationService;
    private final UserService userService;

    public AuthenticationSuccessListener(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        // Вземам текущия потребител от SecurityContext
        String email = event.getAuthentication().getName();
        User user = userService.findUserByEmail(email);

        // Извиквам метода за проверка на предпочитанията за известия
        notificationService.checkNotificationPreference(user.getId(), user.getEmail());
    }
}

