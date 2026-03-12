package cardindex.dojocardindex.web;


import cardindex.dojocardindex.UserConsent.model.UserConsent;
import cardindex.dojocardindex.UserConsent.service.UserConsentService;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/parent-consent")
public class ParentConsentController {

    private final UserConsentService userConsentService;

    public ParentConsentController(UserConsentService userConsentService) {
        this.userConsentService = userConsentService;
    }

    @GetMapping("/verify")
    public ModelAndView showParentConsent(@Param("token") String token) {
        UserConsent consent = userConsentService.getConsentByToken(token);

        ModelAndView modelAndView = new ModelAndView("parent-consent-verify");
        modelAndView.addObject("consent", consent);
        modelAndView.addObject("agreement", consent.getAgreement());
        modelAndView.addObject("token", token);
        return modelAndView;
    }

    @PostMapping("/confirm")
    public String confirmParentConsent(@RequestParam("token") String token) {
        userConsentService.verifyParentConsent(token);
        return "redirect:/parent-consent/success";
    }

    @GetMapping("/success")
    public String success() {
        return "parent-consent-success";
    }

}
