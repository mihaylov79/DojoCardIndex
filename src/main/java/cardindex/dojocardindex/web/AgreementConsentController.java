package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Agreement.model.Agreement;
import cardindex.dojocardindex.Agreement.service.AgreementService;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.UserConsent.model.UserConsent;
import cardindex.dojocardindex.UserConsent.service.ConsentActionResult;
import cardindex.dojocardindex.UserConsent.service.UserConsentService;
import cardindex.dojocardindex.UserConsentHistory.model.UserConsentHistory;
import cardindex.dojocardindex.UserConsentHistory.service.UserConsentHistoryService;
import cardindex.dojocardindex.security.CustomUserDetails;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/consent")
public class AgreementConsentController {
    
    private final AgreementService agreementService;
    private final UserService userService;
    private final UserConsentService userConsentService;
    private final UserConsentHistoryService userConsentHistoryService;

    @Autowired
    public AgreementConsentController(AgreementService agreementService, UserService userService, UserConsentService userConsentService, UserConsentHistoryService userConsentHistoryService) {
        this.agreementService = agreementService;
        this.userService = userService;
        this.userConsentService = userConsentService;
        this.userConsentHistoryService = userConsentHistoryService;
    }

    @GetMapping("/my-consents")
    public ModelAndView getMyConsents(@AuthenticationPrincipal CustomUserDetails customUserDetails) {

        User user = userService.getUserById(customUserDetails.getId());

       UserConsent currentConsent = userConsentService.getUserActiveConsent(user).orElse(null);

        ModelAndView modelAndView = new ModelAndView("my-consents");
        modelAndView.addObject("currentConsent", currentConsent);
        modelAndView.addObject("consentAuditTrail", userConsentService.getUserConsents(user));
        modelAndView.addObject("canRevokeConsent", userConsentService.canRevokeConsent(currentConsent));

        return modelAndView;
    }

    @GetMapping("/my-consents/details/{consentId}")
    public ModelAndView getConsentDetails(@PathVariable UUID consentId, @AuthenticationPrincipal CustomUserDetails details) {
        User user = userService.getUserById(details.getId());
        UserConsent consent = userConsentService.getConsentById(consentId);

        userConsentService.validateUserAccessToConsent(consent, user);

        List<UserConsentHistory> auditTrail = userConsentHistoryService.getHistoryForConsent(consent);

        ModelAndView modelAndView = new ModelAndView("consent-details");
        modelAndView.addObject("consent", consent);
        modelAndView.addObject("auditTrail", auditTrail);
        return modelAndView;
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
            case CONSENT_CANCELED -> "redirect:/consent/canceled";
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

    @GetMapping("/canceled")
    public String alreadyCanceled(){
        return "already-canceled";
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

    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @GetMapping("/show/all")
    public ModelAndView showAllConsents() {
        List<UserConsent> allConsents = userConsentService.getAllConsents();
        ModelAndView modelAndView = new ModelAndView("all-consents");
        modelAndView.addObject("allConsents", allConsents);
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @PostMapping("/admin/cancel/{consentId}")
    public ModelAndView cancelConsent(@PathVariable UUID consentId) {

        UserConsent consent = userConsentService.getConsentById(consentId);

        if(consent.isMinor()) {
            userConsentService.cancelConsentByParent(consentId);
        } else {
            userConsentService.cancelUserConsentByAdmin(consentId);
        }

            return new ModelAndView("redirect:/consent/show/all");

    }


    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @GetMapping("/failed-mails")
    public ModelAndView getFailedMails(){
        List<UserConsent> failedInvitationMails = userConsentService.getConsentsInvitationFailedMails();
        List<UserConsent> failedConfirmationMails = userConsentService.getConsentsConfirmationFailedMails();
        List<UserConsent> failedCancellationMails = userConsentService.getConsentsCancellationFailedMails();

        ModelAndView modelAndView = new ModelAndView("failed-mails");
        modelAndView.addObject("failedInvitationMails",failedInvitationMails);
        modelAndView.addObject("failedConfirmationMails", failedConfirmationMails);
        modelAndView.addObject("failedCancellationMails", failedCancellationMails);
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @PostMapping("/admin/resend/{consentId}")
    public String resendMail(@PathVariable UUID consentId, @RequestParam String type) {
        userConsentService.resendMail(consentId, type);
        return "redirect:/consent/failed-mails";
    }

    @GetMapping("/users/consent-history/details/{userId}")
    public ModelAndView getConsentHistoryDetails(@PathVariable UUID userId){

        User user = userService.getUserById(userId);
        Map<String, List<UserConsentHistory>> historyByAgreement = userConsentHistoryService.getHistoryForUserGroupedByAgreementTitle(user);
        int eventCount = historyByAgreement.values().stream().mapToInt(List::size).sum();
        ModelAndView modelAndView = new ModelAndView("consent-history-details");
        modelAndView.addObject("user", user);
        modelAndView.addObject("historyByAgreement", historyByAgreement);
        modelAndView.addObject("eventCount", eventCount);
        return modelAndView;
    }
}

