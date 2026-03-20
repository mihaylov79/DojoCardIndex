package cardindex.dojocardindex.web;


import cardindex.dojocardindex.Agreement.model.Agreement;
import cardindex.dojocardindex.Agreement.service.AgreementService;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.web.dto.AgreementRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/agreements")
public class AgreementController {
    
    private final AgreementService agreementService;
    private final UserService userService;

    public AgreementController(AgreementService agreementService, UserService userService) {
        this.agreementService = agreementService;
        this.userService = userService;
    }

    @GetMapping("/show-all")
    public ModelAndView getAllAgreements(){
        List<Agreement> agreements = agreementService.getAllAgreements();

        ModelAndView modelAndView = new ModelAndView("agreements");
        modelAndView.addObject("agreements", agreements);
        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @GetMapping("/create/new")
    public ModelAndView getAgreementCreatePage(){

        ModelAndView modelAndView = new ModelAndView("create-agreement");
        modelAndView.addObject("agreementRequest", new AgreementRequest());

        return modelAndView;
    }

    @PreAuthorize("hasAnyRole('TRAINER', 'ADMIN')")
    @PostMapping("/create/new")
    public ModelAndView createAgreement(@Valid AgreementRequest agreementRequest,
                                        BindingResult result){

        if (result.hasErrors()){
            return new ModelAndView("create-agreement");
        }
        agreementService.createAgreement(agreementRequest);
        return new ModelAndView("redirect:/agreements/show-all");

    }

    @PostMapping("/publish/{agreementId}")
    public String publishAgreement(@PathVariable UUID agreementId){
        agreementService.publishNewAgreement(agreementId);

        return "redirect:/agreements/show-all";
    }


}
