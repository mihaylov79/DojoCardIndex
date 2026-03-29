package cardindex.dojocardindex.web;


import cardindex.dojocardindex.UserConsent.model.UserConsent;
import cardindex.dojocardindex.UserConsent.service.UserConsentService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/parent-consent")
public class ParentConsentController {

    private final UserConsentService userConsentService;

    public ParentConsentController(UserConsentService userConsentService) {
        this.userConsentService = userConsentService;
    }

    @GetMapping("/{token}")
    public ModelAndView showParentConsentForm(@PathVariable String token) {
        UserConsent consent = userConsentService.getConsentByToken(token);
        ModelAndView modelAndView = new ModelAndView("parent-consent-form");
        modelAndView.addObject("agreement", consent.getAgreement());
        modelAndView.addObject("token", token);
        return modelAndView;
    }

    @PostMapping("/{token}/accept")
    public ModelAndView acceptParentConsent(@PathVariable String token) {
        UserConsent verifiedConsent = userConsentService.verifyParentConsent(token);
        ModelAndView modelAndView = new ModelAndView("parent-consent-success");
        modelAndView.addObject("agreementTitle", verifiedConsent.getAgreement().getTitle());
        modelAndView.addObject("consentId", verifiedConsent.getId());
        return modelAndView;
    }

    @GetMapping("/{token}/refuse")
    public ModelAndView showRefuseWarning(@PathVariable String token) {
        ModelAndView modelAndView = new ModelAndView("parent-consent-refuse-warning");
        modelAndView.addObject("token", token);
        return modelAndView;
    }

    @PostMapping("/{token}/refuse")
    public String refuseParentConsent(@PathVariable String token, RedirectAttributes redirectAttributes) {
        UserConsent consent = userConsentService.getConsentByToken(token);
        userConsentService.refuseConsent(consent.getUser());
        redirectAttributes.addFlashAttribute("errorMessage", "Родителското съгласие е отказано. Акаунтът ще бъде деактивиран.");
        return "redirect:/parent-consent-fail";
    }
}
