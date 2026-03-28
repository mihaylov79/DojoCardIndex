package cardindex.dojocardindex.config;

import cardindex.dojocardindex.Agreement.service.AgreementService;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.UserConsent.service.UserConsentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ConsentCheckInterceptor implements HandlerInterceptor {

    private final UserConsentService userConsentService;
    private final UserService userService;
    private final AgreementService agreementService;

    public ConsentCheckInterceptor(@Lazy UserConsentService userConsentService,
                                   @Lazy UserService userService,
                                   @Lazy AgreementService agreementService) {
        this.userConsentService = userConsentService;
        this.userService = userService;
        this.agreementService = agreementService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        // Пропускаме проверката за публични и consent страници
        if (isExcludedPath(requestURI)) {
            return true;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }

        // Проверяваме дали има активно споразумение
        if (agreementService.getActiveAgreement().isEmpty()) {
            return true;
        }

        String email = auth.getName();
        User user = userService.findUserByEmail(email);

        // Централизирана проверка за изчакване на родителско съгласие
        if (userConsentService.isWaitingForParentConsent(user)) {
            if (!requestURI.equals("/consent/pending-parent")) {
                response.sendRedirect("/consent/pending-parent");
                return false;
            }
        }

        // Проверяваме дали потребителят има валидно съгласие
        if (!userConsentService.hasValidConsent(user)) {
            // Защита от infinite loop
            if (!requestURI.equals("/consent/show")) {
                response.sendRedirect("/consent/show");
                return false;
            }
        }

        return true;
    }

    private boolean isExcludedPath(String path) {
        // Изключваме публични страници и consent-свързани URL-и
        return path.startsWith("/consent/") ||
                path.startsWith("/parent-consent/") ||
                path.equals("/") ||
                path.equals("/login") ||
                path.equals("/register") ||
                path.equals("/about-us") ||
                path.startsWith("/forgotten-password/") ||
                path.startsWith("/actuator/") ||
                path.equals("/logout") ||
                path.equals("/error") ||
                // Static ресурси
                path.matches(".*\\.(css|js|png|jpg|jpeg|gif|ico|woff|woff2|ttf|svg)$");
    }
}