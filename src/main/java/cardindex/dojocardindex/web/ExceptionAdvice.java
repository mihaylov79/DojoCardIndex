package cardindex.dojocardindex.web;

import cardindex.dojocardindex.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.nio.file.AccessDeniedException;

@ControllerAdvice
public class ExceptionAdvice {


    @ExceptionHandler(UserAlreadyExistException.class)
    public String handleUserAlreadyExist(Exception exception, RedirectAttributes redirectAttributes){

        redirectAttributes.addFlashAttribute("userAlreadyExistMessage",exception.getMessage());
        return "redirect:/register";
    }

    @ExceptionHandler({PasswordDoesNotMatchException.class,
                      IllegalTokenException.class})
    public String handleForgottenPasswordExceptions(Exception exception,
                                                    RedirectAttributes redirectAttributes,
                                                    HttpServletRequest request){

        redirectAttributes.addFlashAttribute("exceptionMessage", exception.getMessage());

        String token = request.getParameter("token");
        if (token != null) {
            return "redirect:/forgotten-password/reset?token=" + token;
        }

        return "redirect:/forgotten-password";
    }


    @ExceptionHandler(EmailAlreadyInUseException.class)
    public String handleEmailAlreadyInUse(Exception exception, RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("emailAlreadyInUse",exception.getMessage());

        return "redirect:/admin/add-user";
    }

    @ExceptionHandler(MessageCanNotBeSentToUserException.class)
    public String handleMessageCannotBeSentToUser(Exception exception,RedirectAttributes redirectAttributes){

        redirectAttributes.addFlashAttribute("messageCanNotBeSentToUser",exception.getMessage());

        return "redirect:/messages/send";
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({EventNotFoundException.class,
                       MessageNotFoundException.class,
                       RequestNotFoundException.class,
                       RequestAlreadyExistException.class,
                       UserNotFoundException.class,
                       IllegalUserStatusException.class,
                       ExportIOException.class,
                       IllegalEventOperationException.class})
    public ModelAndView handleBadRequestException(Exception exception){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("bad-request");
        modelAndView.addObject("errorMessage",exception.getMessage());
        modelAndView.addObject("errorType", HttpStatus.BAD_REQUEST);
        modelAndView.addObject("statusCode", HttpStatusCode.valueOf(400));
        modelAndView.addObject("exception",exception.getClass().getSimpleName());

        return modelAndView;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({AccessDeniedException.class,
                      NoResourceFoundException.class,
                      MethodArgumentTypeMismatchException.class,
                      MissingRequestValueException.class})
    public ModelAndView handleNotFoundExceptions(Exception ex){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("not-found");
        modelAndView.addObject("errorMessage", ex.getMessage());
        modelAndView.addObject("exceptionType", ex.getClass().getSimpleName());
        return modelAndView;

    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnknownExceptions(Exception exception) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("internal-server-error");
        modelAndView.addObject("exceptionMessage", exception.getMessage());
        modelAndView.addObject("messageError", exception.getClass().getSimpleName());

        return modelAndView;
    }
}


