package cardindex.dojocardindex.config;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.UserConsent.service.UserConsentService;
import cardindex.dojocardindex.notification.service.NotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuthenticationSuccessListener {

    private final NotificationService notificationService;
    private final UserService userService;
    private final UserConsentService userConsentService;

    public AuthenticationSuccessListener(NotificationService notificationService, UserService userService, UserConsentService userConsentService) {
        this.notificationService = notificationService;
        this.userService = userService;
        this.userConsentService = userConsentService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        // Вземам текущия потребител от SecurityContext
        String email = event.getAuthentication().getName();
        User user = userService.findUserByEmail(email);

        // Извиквам метода за проверка на предпочитанията за известия
        notificationService.checkNotificationPreference(user.getId(), user.getEmail());

        // Проверка дали чака родителско съгласие
        if (userConsentService.isWaitingForParentConsent(user)) {
            HttpSession session = ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes())
                    .getRequest().getSession();
            session.setAttribute("redirectAfterLogin", "/consent/pending-parent");
            return;
        }
        // Проверка дали има валиден consent само ако има активен Agreement
        if (!userConsentService.hasValidConsent(user)) {
            HttpSession session = ((ServletRequestAttributes)
                    RequestContextHolder.currentRequestAttributes())
                    .getRequest().getSession();
            session.setAttribute("redirectAfterLogin", "/consent/show");
        }

    }
}
