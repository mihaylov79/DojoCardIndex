package cardindex.dojocardindex.web;

import cardindex.dojocardindex.UserConsent.exception.ParentConsentAlreadyConfirmedException;
import cardindex.dojocardindex.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;



@Slf4j
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

    @ExceptionHandler({EventClosedException.class,
                       RequestAlreadyExistException.class})
    public String handleEventClosed(Exception exception,RedirectAttributes redirectAttributes){

        redirectAttributes.addFlashAttribute("eventClosedMessage",exception.getMessage());

        return "redirect:/events";
    }

    @ExceptionHandler(InvalidImageFileException.class)
    public String handleInvalidImageFileException(InvalidImageFileException e,
                                                   RedirectAttributes redirectAttributes,
                                                   HttpServletRequest request){

        log.warn("Невалиден image файл: {}", e.getMessage());

        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        return getRedirectUrl(request);
    }

    @ExceptionHandler(InvalidDocumentFileException.class)
    public String handleInvalidDocumentFileException(InvalidDocumentFileException e,
                                                     RedirectAttributes redirectAttributes){

        log.warn("Невалиден файл: {}", e.getMessage());

        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        return "redirect:/documents/upload";
    }

    @ExceptionHandler(ProfilePictureAccessDeniedException.class)
    public String handleProfilePictureAccessDeniedException(ProfilePictureAccessDeniedException e,
                                                       RedirectAttributes redirectAttributes,
                                                       HttpServletRequest request){

        log.warn("Access Denied: {} at {}", e.getMessage(), request.getRequestURI());

        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        return getRedirectUrl(request);
    }

    @ExceptionHandler(ImageUploadException.class)
    public String handleImageUploadException(ImageUploadException e,
                                             RedirectAttributes redirectAttributes,
                                             HttpServletRequest request){

        log.error("Грешка при качване на изображение", e);

        // Опростено - показваме съобщението от exception-а
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        return getRedirectUrl(request);
    }

    @ExceptionHandler(FileUploadException.class)
    public String handleFileUploadException(FileUploadException e,
                                            RedirectAttributes redirectAttributes){

        log.error("Грешка при качването на файл",e);

        String userMessage = e.getMessage();

        if (userMessage == null || userMessage.isEmpty()){
            userMessage = "Грешка при качване на файла. Моля опитайте отново.";
        }
        redirectAttributes.addFlashAttribute("errorMessage", userMessage);

        return "redirect:/documents/upload";
    }

    @ExceptionHandler(ImageDeleteException.class)
    public String handleImageDeleteException(ImageDeleteException e,
                                             RedirectAttributes redirectAttributes,
                                             HttpServletRequest request){

        log.error("Грешка при изтриване на изображение", e);

        redirectAttributes.addFlashAttribute("errorMessage",
                "Възникна грешка при изтриване на старата снимка. Моля опитайте отново.");

        return getRedirectUrl(request);
    }

    @ExceptionHandler(FileDeleteException.class)
    public String handleFileDeleteException(FileDeleteException e,
                                                  RedirectAttributes redirectAttributes){

        log.error("Грешка при изтриване на файл", e);

        redirectAttributes.addFlashAttribute("errorMessage",
                "Грешка при изтриване на файла. Моля опитайте отново.");

        return "redirect:/documents";
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public String handleDocumentNotFoundException(DocumentNotFoundException e,
                                                  RedirectAttributes redirectAttributes){

        log.warn("Документ не е намерен: {}", e.getMessage());

        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());

        return "redirect:/documents";
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e,
                                                       RedirectAttributes redirectAttributes,
                                                       HttpServletRequest request){

        log.warn("Файлът е твърде голям: {}", e.getMessage());

        redirectAttributes.addFlashAttribute("errorMessage",
                "Файлът е твърде голям! Максималният размер не може да надвишава 5MB.");

        return getRedirectUrl(request);
    }

    @ExceptionHandler(InvalidUserConsentException.class)
    public String handleInvalidUserConsentException(InvalidUserConsentException e,
                                                    RedirectAttributes redirectAttributes){
        log.error("Няма мамерема заявка за съгласие този токен - {}", e.getMessage());

        redirectAttributes.addFlashAttribute("errorMessage","Няма мамерема заявка за съгласие този токен");

        return "redirect:/parent-consent/verify";
    }

    @ExceptionHandler(AgreementNotFoundException.class)
    public String handleAgreementNotFoundException(AgreementNotFoundException e,
                                                   RedirectAttributes redirectAttributes,
                                                   HttpServletRequest request){
        log.warn("Активно споразумение не е намерено: {}", e.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return getRedirectUrl(request);
    }

    @ExceptionHandler(TokenNotFoundException.class)
    public ModelAndView handleTokenNotFound(TokenNotFoundException e) {
        ModelAndView modelAndView = new ModelAndView("parent-consent-fail");
        modelAndView.addObject("errorMessage", e.getMessage());
        return modelAndView;
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ModelAndView handleTokenExpired(TokenExpiredException e) {
        ModelAndView modelAndView = new ModelAndView("parent-consent-fail");
        modelAndView.addObject("errorMessage", e.getMessage());
        return modelAndView;
    }

    @ExceptionHandler(ParentConsentAlreadyConfirmedException.class)
    public ModelAndView handleParentConsentAlreadyConfirmed(ParentConsentAlreadyConfirmedException ex) {
        ModelAndView modelAndView = new ModelAndView("parent-consent-fail");
        modelAndView.addObject("errorMessage", ex.getMessage());
        return modelAndView;
    }

    /**
     * Помощен метод за извличане на URL за redirect.
     * Връща Referer URL ако съществува, иначе /home.
     */
    private String getRedirectUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");

        if (referer != null && !referer.isEmpty()) {
            // Извличаме само пътя след domain-а
            try {
                String path = referer.substring(referer.indexOf(request.getContextPath()));
                log.debug("Redirecting back to: {}", path);
                return "redirect:" + path;
            } catch (Exception e) {
                log.warn("Не може да се извлече Referer path, използва се /home");
            }
        }

        return "redirect:/home";
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({EventNotFoundException.class,
                       MessageNotFoundException.class,
                       RequestNotFoundException.class,
                       UserNotFoundException.class,
                       IllegalUserStatusException.class,
                       ExportIOException.class,
                       IllegalEventOperationException.class})
    public ModelAndView handleBadRequestException(Exception exception, HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("bad-request");
        modelAndView.addObject("errorMessage",exception.getMessage());

        log.warn("Handled 400-type exception: {} at [{}], Message: {}",
                exception.getClass().getSimpleName(),
                request.getRequestURI(),
                exception.getMessage());

        return modelAndView;
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({AccessDeniedException.class,
                      NoResourceFoundException.class,
                      MethodArgumentTypeMismatchException.class,
                      MissingRequestValueException.class})
    public ModelAndView handleNotFoundExceptions(Exception ex, HttpServletRequest request){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("not-found");

        log.warn("Handled 404-type exception: {} at [{}], Message: {}",
                ex.getClass().getSimpleName(),
                request.getRequestURI(),
                ex.getMessage());

        return modelAndView;

    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ModelAndView handleUnknownExceptions(Exception exception, HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("internal-server-error");
        modelAndView.addObject("exceptionMessage", exception.getMessage());
        modelAndView.addObject("messageError", exception.getClass().getSimpleName());

        log.warn("Handle 500-type exception {} at [{}], Message: {}",
                exception.getClass().getSimpleName(),
                request.getRequestURI(),
                exception.getMessage());

        return modelAndView;
    }
}


