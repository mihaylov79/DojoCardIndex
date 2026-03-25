package cardindex.dojocardindex.web;


import cardindex.dojocardindex.UserConsent.model.UserConsent;
import cardindex.dojocardindex.UserConsent.service.UserConsentService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/parent-consent")
public class ParentConsentController {

    private final UserConsentService userConsentService;

    public ParentConsentController(UserConsentService userConsentService) {
        this.userConsentService = userConsentService;
    }

    @GetMapping("/{token}")
    public ModelAndView handleParentConsent(@PathVariable String token) {
        UserConsent verifiedConsent = userConsentService.verifyParentConsent(token);
        ModelAndView modelAndView = new ModelAndView("parent-consent-success");
        modelAndView.addObject("agreementTitle", verifiedConsent.getAgreement().getTitle());
        modelAndView.addObject("consentId", verifiedConsent.getId());
        return modelAndView;
    }
}
