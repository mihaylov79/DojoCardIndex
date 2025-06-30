package cardindex.dojocardindex.web;

import cardindex.dojocardindex.ForgottenPasswordToken.service.ForgottenPasswordTokenService;
import cardindex.dojocardindex.web.dto.ForgottenPasswordEmailRequest;
import cardindex.dojocardindex.web.dto.ResetForgottenPasswordRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/forgotten-password")
public class ForgottenPasswordTokenController {

    private final ForgottenPasswordTokenService tokenService;

    @Autowired
    public ForgottenPasswordTokenController(ForgottenPasswordTokenService tokenService) {
        this.tokenService = tokenService;
    }

@GetMapping("/reset")
public ModelAndView getForgottenPasswordResetPage(@RequestParam("token") String token){

        ModelAndView modelAndView = new ModelAndView("reset-password-form");
        modelAndView.addObject("token", token);
        modelAndView.addObject("resetForgottenPasswordRequest", new ResetForgottenPasswordRequest());

        return modelAndView;
}

@PostMapping("/reset")
public ModelAndView resetPassword(@RequestParam("token") String token,
                                  @Valid ResetForgottenPasswordRequest request,
                                  BindingResult result){

        if (result.hasErrors()){
            return new ModelAndView("reset-password-form");

        }

        tokenService.resetForgottenPassword(token,request);

        return  new ModelAndView("reset-password-success");

}

@GetMapping
public ModelAndView getForgottenPasswordPage() {

        ModelAndView modelAndView = new ModelAndView("forgotten-password-form");
        modelAndView.addObject("forgottenPasswordEmailRequest", new ForgottenPasswordEmailRequest());

        return modelAndView;

}

@PostMapping
public ModelAndView sendForgottenPasswordToken(@Valid ForgottenPasswordEmailRequest request, BindingResult result) {

        if (result.hasErrors()){

            return new ModelAndView("forgotten-password-form");
        }

        tokenService.sendForgottenPasswordEmail(request.getEmail());

        return new ModelAndView("forgotten-password-success");
}

}
