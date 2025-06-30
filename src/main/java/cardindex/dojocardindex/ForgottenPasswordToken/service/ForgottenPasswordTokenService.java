package cardindex.dojocardindex.ForgottenPasswordToken.service;

import cardindex.dojocardindex.ForgottenPasswordToken.models.ForgottenPasswordToken;
import cardindex.dojocardindex.ForgottenPasswordToken.repository.ForgottenPasswordTokenRepository;
import cardindex.dojocardindex.User.models.RegistrationStatus;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.models.UserStatus;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.exceptions.IllegalTokenException;
import cardindex.dojocardindex.exceptions.IllegalUserStatusException;
import cardindex.dojocardindex.exceptions.PasswordDoesNotMatchException;
import cardindex.dojocardindex.notification.client.NotificationClient;
import cardindex.dojocardindex.web.dto.ForgottenPasswordRequest;
import cardindex.dojocardindex.web.dto.ResetForgottenPasswordRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class ForgottenPasswordTokenService {

    private final ForgottenPasswordTokenRepository tokenRepository;
    private  final UserService userService;
    private final NotificationClient notificationClient;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ForgottenPasswordTokenService(ForgottenPasswordTokenRepository tokenRepository, UserService userService, NotificationClient notificationClient, PasswordEncoder passwordEncoder) {
        this.tokenRepository = tokenRepository;
        this.userService = userService;
        this.notificationClient = notificationClient;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void sendForgottenPasswordEmail(String email){

        User user = userService.findUserByEmail(email);

        if (user.getStatus()== UserStatus.INACTIVE){
            throw new IllegalUserStatusException("Този потребителски профил е деактивиран!");
        }

        if (user.getRegistrationStatus() == RegistrationStatus.NOT_REGISTERED
                                        || user.getRegistrationStatus() == RegistrationStatus.PENDING) {

            throw new IllegalUserStatusException("Този потребителски профил няма регистрация или чака потвърждение");
        }

        user.setResetToken(null);
        tokenRepository.deleteByUser(user);

        String token = generateSecureToken();

        String forgottenPasswordLink = "http://localhost:8080/forgotten-password/reset?token=" + token;

        ForgottenPasswordRequest request = ForgottenPasswordRequest.builder()
                .recipient(user.getEmail())
                .title("Заявка за промяма на парола")
                .content(
                        "<p>Здравейте,</p>" +
                        "<p>Получихме заявка за възстановяване на забравена парола.</p>" +
                        "<p><strong>Ако Вие НЕ сте изпратили тази заявка, моля игнорирайте този имейл.</strong></p>" +
                        "<p>Ако желаете да промените паролата си, последвайте следния линк:</p>" +
                         "<p><a href=\"" + forgottenPasswordLink + "\">Промяна на парола</a></p>"
                )
                .build();

        ResponseEntity<Void> response =notificationClient.sendForgottenPasswordEmail(request);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Неуспешно изпращане на заявка за възстановяване на забравена парола.");
        }

        ForgottenPasswordToken forgottenPasswordToken = ForgottenPasswordToken.builder()
                .token(token)
                .user(user)
                .created(LocalDateTime.now())
                .expires(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        tokenRepository.save(forgottenPasswordToken);

    }

    public void resetForgottenPassword (String token , ResetForgottenPasswordRequest request){


        if (!request.getNewPassword().equals(request.getConfirmNewPassword())){

            throw new PasswordDoesNotMatchException();
        }

        ForgottenPasswordToken resetToken = tokenRepository.findByToken(token).orElseThrow(() -> new IllegalTokenException("Този токен не е намерен"));

         if (resetToken.getExpires().isBefore(LocalDateTime.now())){
             throw new IllegalTokenException();
         }

         User user = resetToken.getUser();

         user = user.toBuilder()
                 .password(passwordEncoder.encode(request.getNewPassword()))
                 .build();

         userService.saveUser(user);

         tokenRepository.delete(resetToken);
    }

    private String generateSecureToken(){
        SecureRandom token = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        token.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }


}
