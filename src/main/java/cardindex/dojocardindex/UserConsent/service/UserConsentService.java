package cardindex.dojocardindex.UserConsent.service;

import cardindex.dojocardindex.Agreement.model.Agreement;
import cardindex.dojocardindex.Agreement.service.AgreementService;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.models.UserStatus;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.UserConsent.model.MailSendStatus;
import cardindex.dojocardindex.UserConsent.model.UserConsent;
import cardindex.dojocardindex.UserConsent.repository.UserConsentRepository;
import cardindex.dojocardindex.exceptions.AgreementNotFoundException;
import cardindex.dojocardindex.exceptions.InvalidUserConsentException;
import cardindex.dojocardindex.notification.client.NotificationClient;
import cardindex.dojocardindex.web.dto.ParentConsentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
public class UserConsentService {

    private final UserConsentRepository repository;
    private final AgreementService agreementService;
    private final NotificationClient notificationClient;
    private final UserService userService;

    @Value("${app.base-url}")
    private String baseUrl;

    @Autowired
    public UserConsentService(UserConsentRepository repository, AgreementService agreementService, NotificationClient notificationClient, UserService userService) {
        this.repository = repository;
        this.agreementService = agreementService;
        this.notificationClient = notificationClient;
        this.userService = userService;
    }
    
    @Autowired
    private UserConsentService self;
    
    public boolean hasValidConsent(User user){
        return repository.findByUser(user)
                .map(UserConsent::isFullyConsented)
                .orElse(false);
    }
    
    private void acceptDirectConsent(User user){
        Agreement agreement;
        try {
            agreement = agreementService.getActiveAgreement();
        } catch (Exception e) {
            log.warn("[CONSENT] Пропуснато директно съгласие за потребител {} ({}), защото няма активен Agreement.", user.getId(), user.getEmail());
            return;
        }
        UserConsent consent = UserConsent.builder()
                .user(user)
                .agreement(agreement)
                .agreedAt(LocalDateTime.now())
                .isMinor(false)
                .pending(false)
                .finished(true)
                .sentMailStatus(MailSendStatus.UNNECESSARY)
                .build();
        
        repository.save(consent);
    }
    
    
    
    @Transactional
    public void initiateParentConsent(User user) {
        Agreement agreement;
        try {
            agreement = agreementService.getActiveAgreement();
        } catch (Exception e) {
            log.warn("[CONSENT] Мейла за родителско съгласие за потребител {} ({}), не беше изпратен поради липса на активено споразумение(Agreement).", user.getId(), user.getEmail());
            throw new AgreementNotFoundException("Невъзможно е да се инициира родителско съгласие, защото няма активно споразумение (Agreement). Моля свържете се с администратор.");

        }
        String token = generateSecureToken();
        UserConsent consent = UserConsent.builder()
                .user(user)
                .agreement(agreement)
                .agreedAt(LocalDateTime.now())
                .isMinor(true)
                .parentEmail(user.getContactPersonEmail())
                .consentToken(token)
                .tokenExpiresAt(LocalDateTime.now().plusHours(48))
                .pending(false)
                .finished(false)
                .sentMailStatus(MailSendStatus.SENT)
                .build();
        repository.save(consent);

        ParentConsentRequest emailRequest = ParentConsentRequest.builder()
                .parentEmail(user.getContactPersonEmail())
                .childFirstName(user.getFirstName())
                .childLastName(user.getLastName())
                .agreementContent(agreement.getContent())
                .consentLink(baseUrl + "/parent-consent/" + token)
                .build();
        sendParentConsentEmailWithStatus(consent, emailRequest, user);
    }
    
    @Transactional
    public UserConsent verifyParentConsent(String token){
        
        UserConsent consent = repository.findByConsentToken(token)
                .orElseThrow(() -> new RuntimeException("Невалиден токен!"));
        
        if (!consent.isTokenValid()){
            throw new RuntimeException("Токенът е изтекъл! Моля свържете се с администратор");
        }
        
            return repository.save(consent.toBuilder()
                    .parentConsentedAt(LocalDateTime.now())
                    .pending(false)
                    .pendingReason(null)
                    .finished(true)
                    .build());
            
        
    }
    
    public void regenerateParentConsentToken(User user) {
        UserConsent consent = repository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Няма намерено съгласие за потребителя!"));
        if (consent.getParentConsentedAt() != null){
            throw new RuntimeException("Родителят вече е потвърдил - не е нужен токен");
        }
        String newToken = generateSecureToken();
        consent = consent.toBuilder()
                .consentToken(newToken)
                .tokenExpiresAt(LocalDateTime.now().plusHours(24))
                .build();
        repository.save(consent);

        Agreement agreement;
        try {
            agreement = agreementService.getActiveAgreement();
        } catch (Exception e) {
            log.warn("[CONSENT] Пропуснато родителско съгласие за потребител {} ({}), защото няма активен Agreement.", user.getId(), user.getEmail());
            throw new AgreementNotFoundException("Невъзможно е да се изпрати родителско съгласие, защото няма активно споразумение (Agreement). Моля свържете се с администратор.");
        }
        ParentConsentRequest emailRequest = ParentConsentRequest.builder()
                .parentEmail(user.getContactPersonEmail())
                .childFirstName(user.getFirstName())
                .childLastName(user.getLastName())
                .consentLink(baseUrl + "/parent-consent/" + newToken)
                .agreementContent(agreement.getContent())
                .build();
        sendParentConsentEmailWithStatus(consent, emailRequest, user);
    }
    
    @Transactional
    public void refuseConsent(User user) {
        User deactivated = user.toBuilder()
                .status(UserStatus.INACTIVE)
                .build();
        userService.saveUser(deactivated);
    }
    
    @Transactional
    public void createPendingConsent(User user, String reason) {
        Agreement agreement;
        try {
            agreement = agreementService.getActiveAgreement();
        } catch (Exception e) {
            log.warn("[CONSENT] Пропуснато временно съгласие за потребител {} ({}), защото няма активен Agreement.", user.getId(), user.getEmail());
            throw new AgreementNotFoundException("Невъзможно е да се създаде временно съгласие, защото няма активно споразумение (Agreement). Моля свържете се с администратор.");
        }
        UserConsent consent = UserConsent.builder()
                .user(user)
                .agreement(agreement)
                .agreedAt(null)
                .isMinor(false)
                .parentEmail(user.getContactPersonEmail())
                .pending(true)
                .pendingReason(reason)
                .finished(false)
                .sentMailStatus(MailSendStatus.UNNECESSARY)
                .build();
        
        repository.save(consent);
    }

    public List<UserConsent>getBlockedConsents(){
        return repository.findAllByFinishedFalseAndPendingFalseOrderByCreatedAtDesc();
    }
    public List<UserConsent> getAllConsents(){
        return repository.findAll(Sort.by(Sort.Direction.DESC, "agreedAt"));
    }

    public List<UserConsent>getConsentsFailedMails(){
        return repository.findAllBySentMailStatusFailed().stream().toList();
    }

    public List<UserConsent>getPendingConsents(){
        return repository.findAllByPendingTrueOrderByAgreedAtDesc();
    }

    public boolean isMinor(User user) {
        if (user.getBirthDate() == null) return false;
        return Period.between(user.getBirthDate(), LocalDate.now()).getYears() < 18;
    }

    public UserConsent getConsentByToken(String token){

        return repository.findByConsentToken(token)
                .orElseThrow(()-> new InvalidUserConsentException("Няма заявка за съгласие с токен : [%s]".formatted(token)));
    }


    private String generateSecureToken(){
        SecureRandom token = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        token.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        
    }

    private void sendParentConsentEmailWithStatus(UserConsent consent, ParentConsentRequest emailRequest, User user) {
        try {
            notificationClient.sendParentConsentEmail(emailRequest);
        } catch (Exception e) {
            consent = consent.toBuilder()
                    .sentMailStatus(MailSendStatus.FAILED)
                    .build();
            repository.save(consent);
            log.error("Грешка при изпращане на родителски имейл за потребител {}: {}", user.getId(), e.getMessage(), e);
        }
    }

    public ConsentActionResult processConsentAcceptance(User user) {
        Agreement agreement;
        try {
            agreement = agreementService.getActiveAgreement();
        } catch (Exception e) {
            log.warn("[CONSENT] Пропуснато изискване за съгласие при вход за потребител {} ({}), защото няма активен Agreement.", user.getId(), user.getEmail());
            return ConsentActionResult.CONSENT_ACCEPTED;
        }
        if (isMinor(user)) {
            if (user.getContactPersonEmail() == null || user.getContactPersonEmail().isBlank()) {
                return ConsentActionResult.NO_PARENT_EMAIL;
            }
            self.initiateParentConsent(user);
            return ConsentActionResult.PENDING_PARENT;
        }
        acceptDirectConsent(user);
        return ConsentActionResult.CONSENT_ACCEPTED;
    }

    public boolean isParentConsentConfirmed(User user) {
        return repository.findByUser(user)
                .map(c -> c.getParentConsentedAt() != null)
                .orElse(false);
    }

    public boolean isParentConsentTokenExpired(User user) {
        return repository.findByUser(user)
                .map(c -> !c.isTokenValid())
                .orElse(false);
    }
}
