package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Agreement.service.AgreementService;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.UserConsent.service.ConsentActionResult;
import cardindex.dojocardindex.UserConsent.service.UserConsentService;
import cardindex.dojocardindex.security.CustomUserDetails;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/consent")
public class AgreementConsentController {
    
    private final AgreementService agreementService;
    private final UserService userService;
    private final UserConsentService userConsentService;

    @Autowired
    public AgreementConsentController(AgreementService agreementService, UserService userService, UserConsentService userConsentService) {
        this.agreementService = agreementService;
        this.userService = userService;
        this.userConsentService = userConsentService;
    }
    
    @GetMapping
    public ModelAndView showConsent() {
        ModelAndView modelAndView = new ModelAndView("consent-show");
        modelAndView.addObject("agreement", agreementService.getActiveAgreement());
        return modelAndView;
    }
    
    @PostMapping("/accept")
    public String acceptConsent(@AuthenticationPrincipal CustomUserDetails details) {
        User user = userService.findUserByEmail(details.getEmail());
        ConsentActionResult result = userConsentService.processConsentAcceptance(user);
        return switch (result) {
            case NO_PARENT_EMAIL -> "redirect:/consent/no-parent-email";
            case PENDING_PARENT -> "redirect:/consent/pending-parent";
            default -> "redirect:/home";
        };
    }
    
    @GetMapping("/refuse")
    public String showRefuseWarning() {
        return "consent-refuse-warning";
    }
    
    @PostMapping("/refuse")
    public String refuseContent(@AuthenticationPrincipal CustomUserDetails details,
                                HttpSession session) {
        User user = userService.getUserById(details.getId());
        userConsentService.refuseConsent(user);
        session.invalidate();
        return "redirect:/login?consentRefused";
    }
    
    @GetMapping("/pending-parent")
    public ModelAndView pendingParent(@AuthenticationPrincipal CustomUserDetails details) {
        User user = userService.findUserByEmail(details.getEmail());
        if (userConsentService.isParentConsentConfirmed(user)) {
            return new ModelAndView("redirect:/home");
        }
        ModelAndView modelAndView = new ModelAndView("consent-pending-parent");
        modelAndView.addObject("tokenExpired", userConsentService.isParentConsentTokenExpired(user));
        return modelAndView;
    }

    @PostMapping("/resend-parrent-consent")
    public String resendParentConsent(@AuthenticationPrincipal CustomUserDetails details) {
        User user = userService.getUserById(details.getId());
        userConsentService.regenerateParentConsentToken(user);
        return "redirect:/consent/pending-parent";
    }
    
    @GetMapping("/no-parent-email")
    public String noParentEmail() {
        return "consent-no-parent-email";
    }
}

