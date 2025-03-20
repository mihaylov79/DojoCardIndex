package cardindex.dojocardindex.web;

import cardindex.dojocardindex.exceptions.UserAlreadyExistException;
import cardindex.dojocardindex.exceptions.UserNotFoundException;
import org.springframework.http.HttpStatus;
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

    //TODO Да проверя дали мога да закача трите ексепшъна заедно с един Handler
    @ExceptionHandler({UserAlreadyExistException.class,
                       UserNotFoundException.class})
    public String handleUserAlreadyExist(Exception exception, RedirectAttributes redirectAttributes){

        redirectAttributes.addFlashAttribute("userAlreadyExistMessage",exception.getMessage());
        return "redirect:/register";
    }
   //TODO Да създам страница коятп да подавам при ексепшън!
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({AccessDeniedException.class,
                      NoResourceFoundException.class,
                      MethodArgumentTypeMismatchException.class,
                      MissingRequestValueException.class})
    public ModelAndView handleNotFoundExceptions(Exception ex){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("not-found");
        modelAndView.addObject("errorMessage", ex.getMessage());

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


