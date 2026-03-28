package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Agreement.model.Agreement;
import cardindex.dojocardindex.Agreement.service.AgreementService;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.UserConsent.model.UserConsent;
import cardindex.dojocardindex.UserConsent.service.ConsentActionResult;
import cardindex.dojocardindex.UserConsent.service.UserConsentService;
import cardindex.dojocardindex.security.CustomUserDetails;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    
    @GetMapping("/show")
    public ModelAndView showAgreementForConsent() {
        Optional<Agreement> activeAgreementOpt = agreementService.getActiveAgreement();
        if (activeAgreementOpt.isEmpty()) {
            return new ModelAndView("redirect:/home");
        }
        ModelAndView modelAndView = new ModelAndView("consent-show");
        modelAndView.addObject("agreement", activeAgreementOpt.get());
        return modelAndView;
    }
    
    @PostMapping("/accept")
    public String acceptConsent(@AuthenticationPrincipal CustomUserDetails details) {

        if (agreementService.getActiveAgreement().isEmpty()) {
            return "redirect:/home";
        }
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

        if (agreementService.getActiveAgreement().isEmpty()) {
            return "redirect:/home";
        }
        return "consent-refuse-warning";
    }
    
    @PostMapping("/refuse")
    public String refuseContent(@AuthenticationPrincipal CustomUserDetails details,
                                HttpSession session) {
        if (agreementService.getActiveAgreement().isEmpty()) {
            return "redirect:/home";
        }

        User user = userService.getUserById(details.getId());
//        userConsentService.refuseConsent(user);
        session.invalidate();
        return "redirect:/login?consentRefused";
    }

    @GetMapping("/pending-parent")
    public ModelAndView pendingParent(@AuthenticationPrincipal CustomUserDetails details) {

        if (agreementService.getActiveAgreement().isEmpty()) {
            return new ModelAndView("redirect:/home");
        }

        User user = userService.findUserByEmail(details.getEmail());
        if (userConsentService.isParentConsentConfirmed(user)) {
            return new ModelAndView("redirect:/home");
        }
        ModelAndView modelAndView = new ModelAndView("consent-pending-parent");
        modelAndView.addObject("tokenExpired", userConsentService.isParentConsentTokenExpired(user));
        // Добавяме оставащо време до изтичане на токена
        long tokenSecondsLeft = userConsentService.getParentConsentTokenSecondsLeft(user);
        if (tokenSecondsLeft > 0) {
            modelAndView.addObject("tokenSecondsLeft", tokenSecondsLeft);
        }
        return modelAndView;
    }

    @PostMapping("/resend-parrent-consent")
    public String resendParentConsent(@AuthenticationPrincipal CustomUserDetails details) {

        if (agreementService.getActiveAgreement().isEmpty()) {
            return "redirect:/home";
        }

        User user = userService.getUserById(details.getId());
        userConsentService.regenerateParentConsentToken(user);
        return "redirect:/consent/pending-parent";
    }
    
    @GetMapping("/no-parent-email")
    public String noParentEmail() {
        return "consent-no-parent-email";
    }

    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @GetMapping("/admin/blocked")
    public ModelAndView showBlockedConsents() {

        List<UserConsent> blockedConsents = userConsentService.getBlockedConsents();
        ModelAndView modelAndView = new ModelAndView("blocked-consents");
        modelAndView.addObject("blockedConsents", blockedConsents);
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @GetMapping("/admin/pending")
    public ModelAndView showPendingConsents() {
        List<UserConsent> pendingConsents = userConsentService.getPendingConsents();
        ModelAndView modelAndView = new ModelAndView("pending-consents");
        modelAndView.addObject("pendingConsents", pendingConsents);
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @PostMapping("admin/set-pending/{consentId}")
    public String setPendingStatus(@PathVariable UUID consentId, @RequestParam String reason) {

        userConsentService.setExistingConsentPending(consentId,reason);
        return "redirect:/consent/admin/blocked";
    }

    @GetMapping("/show/all")
    public ModelAndView showAllConsents() {
        List<UserConsent> allConsents = userConsentService.getAllConsents();
        ModelAndView modelAndView = new ModelAndView("all-consents");
        modelAndView.addObject("allConsents", allConsents);
        return modelAndView;
    }
}

