package cardindex.dojocardindex.UserConsent.service;

import cardindex.dojocardindex.Agreement.model.Agreement;
import cardindex.dojocardindex.Agreement.service.AgreementService;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.models.UserStatus;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.UserConsent.exception.ParentConsentAlreadyConfirmedException;
import cardindex.dojocardindex.UserConsent.model.MailSendStatus;
import cardindex.dojocardindex.UserConsent.model.UserConsent;
import cardindex.dojocardindex.UserConsent.repository.UserConsentRepository;
import cardindex.dojocardindex.exceptions.InvalidUserConsentException;
import cardindex.dojocardindex.exceptions.TokenExpiredException;
import cardindex.dojocardindex.exceptions.TokenNotFoundException;
import cardindex.dojocardindex.exceptions.UserConsentNotFoundException;
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
import java.util.Optional;
import java.util.UUID;

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
    
    public boolean hasValidConsent(User user){
        Optional<Agreement> activeAgreementOpt = agreementService.getActiveAgreement();

        if (activeAgreementOpt.isEmpty()){
            return true;
        }
        return repository.findByUserAndAgreement(user, activeAgreementOpt.get())
                .map(UserConsent::isFullyConsented)
                .orElse(false);
    }
    
    private void acceptDirectConsent(User user){
        Optional<Agreement> activeAgreementOpt = getActiveAgreementSafely(user,
                "[CONSENT] Пропуснато директно съгласие за потребител {} ({}), защото няма активно споразумение(Agreement).");

        if (activeAgreementOpt.isEmpty()){
            return;
        }
        UserConsent consent = UserConsent.builder()
                .user(user)
                .agreement(activeAgreementOpt.get())
                .agreedAt(LocalDateTime.now())
                .isMinor(false)
                .pending(false)
                .finished(true)
                .createdAt(LocalDateTime.now())
                .sentMailStatus(MailSendStatus.UNNECESSARY)
                .build();
        
        repository.save(consent);
    }
    
    
    @Transactional
    public void initiateParentConsent(User user) {
        Optional<Agreement> activeAgreementOpt = getActiveAgreementSafely(user,
                "[CONSENT] Невъзможно е да се инициира родителско съгласие за потребител {} ({}), защото няма активно споразумение (Agreement).");

        if (activeAgreementOpt.isEmpty()){
            return;
        }

        Agreement activeAgreement = activeAgreementOpt.get();
        String token = generateSecureToken();
        UserConsent consent = UserConsent.builder()
                .user(user)
                .agreement(activeAgreement)
                .agreedAt(LocalDateTime.now())
                .isMinor(true)
                .parentEmail(user.getContactPersonEmail())
                .consentToken(token)
                .tokenExpiresAt(LocalDateTime.now().plusHours(48))
                .pending(false)
                .finished(false)
                .createdAt(LocalDateTime.now())
                .sentMailStatus(MailSendStatus.SENT)
                .build();
        repository.save(consent);

        ParentConsentRequest emailRequest = ParentConsentRequest.builder()
                .parentEmail(user.getContactPersonEmail())
                .childFirstName(user.getFirstName())
                .childLastName(user.getLastName())
                .agreementContent(activeAgreement.getContent())
                .consentLink(baseUrl + "/parent-consent/" + token)
                .build();
        sendParentConsentEmailWithStatus(consent, emailRequest, user);
    }
    
    @Transactional
    public UserConsent verifyParentConsent(String token){

        UserConsent consent = repository.findByConsentToken(token)
                .orElseThrow(() -> new TokenNotFoundException("Невалиден токен!"));

        if (consent.getParentConsentedAt() != null) {
            throw new ParentConsentAlreadyConfirmedException("Родителското съгласие вече е потвърдено!");
        }

        if (!consent.isTokenValid()){
            throw new TokenExpiredException("Токенът е изтекъл! Моля свържете се с администратор");
        }

        return repository.save(consent.toBuilder()
                .parentConsentedAt(LocalDateTime.now())
                .pending(false)
                .pendingReason(null)
                .finished(true)
                .build());

        
    }
    
    public void regenerateParentConsentToken(User user) {
        Optional<Agreement> activeAgreementOpt = getActiveAgreementSafely(user,
                "[CONSENT] Невъзможно е да се генерира нов токен за родителско съгласие за потребител {} ({}), защото няма активно споразумение (Agreement).");

        if (activeAgreementOpt.isEmpty()){
            return;
        }

        Agreement agreement = activeAgreementOpt.get();


        UserConsent consent = repository.findByUserAndAgreement(user,agreement)
                .orElseThrow(() -> new UserConsentNotFoundException("Няма намерено съгласие за потребителя!"));
        if (consent.getParentConsentedAt() != null){
            throw new ParentConsentAlreadyConfirmedException("Родителят вече е потвърдил - не е нужен токен");
        }
        String newToken = generateSecureToken();
        consent = consent.toBuilder()
                .consentToken(newToken)
                .tokenExpiresAt(LocalDateTime.now().plusHours(24))
                .build();
        repository.save(consent);
        
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
        Optional<Agreement> activeAgreementOpt = getActiveAgreementSafely(user,
                "[CONSENT] Невъзможно е да се създаде временно съгласие за потребител {} ({}), защото няма активно споразумение (Agreement).");

        if (activeAgreementOpt.isEmpty()){
            return;
        }

        UserConsent consent = UserConsent.builder()
                .user(user)
                .agreement(activeAgreementOpt.get())
                .agreedAt(null)
                .isMinor(false)
                .parentEmail(user.getContactPersonEmail())
                .pending(true)
                .pendingReason(reason)
                .finished(false)
                .createdAt(LocalDateTime.now())
                .sentMailStatus(MailSendStatus.UNNECESSARY)
                .build();
        
        repository.save(consent);
    }

    public void setExistingConsentPending(UUID consentId, String reason) {
        UserConsent consent = getConsentById(consentId);
        if (consent.isPending()) {
            throw new RuntimeException("Съгласието вече е в статус 'pending'!");
        }
        consent = consent.toBuilder()
                .pending(true)
                .pendingReason(reason)
                .finished(false)
                .build();
        repository.save(consent);
    }

    public UserConsent getConsentById(UUID consentId){
        return repository.findById(consentId)
                .orElseThrow(() -> new UserConsentNotFoundException("Няма намерено съгласие с идентификация [%s]!".formatted(consentId)));
    }

    public List<UserConsent>getBlockedConsents(){
        return repository.findAllByFinishedFalseAndPendingFalseOrderByCreatedAtDesc();
    }
    public List<UserConsent> getAllConsents(){
        return repository.findAllByOrderByCreatedAtDesc(Sort.by(
            Sort.Order.asc("user.firstName"),
            Sort.Order.asc("user.lastName")
        ));
    }

    public List<UserConsent> getConsentsFailedMails() {
        return repository.findAllBySentMailStatus(MailSendStatus.FAILED);
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


    public ConsentActionResult processConsentAcceptance(User user) {
        Optional<Agreement> activeAgreementOpt = getActiveAgreementSafely(
            user, "[CONSENT] Невъзможно е да се изиска съгласие за потребител {} ({}), защото няма активно споразумение (Agreement)."
        );

        if (activeAgreementOpt.isEmpty()) {
            return ConsentActionResult.CONSENT_ACCEPTED ;
        }

        if (isMinor(user)) {
            if (user.getContactPersonEmail() == null || user.getContactPersonEmail().isBlank()) {
                return ConsentActionResult.NO_PARENT_EMAIL;
            }
            initiateParentConsent(user);
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


    /**
     * Универсален метод за взимане на активен Agreement.
     * Използва се за защита от edge cases – например ако Agreement бъде деактивиран по време на процеса на съгласие.
     * Ако няма активен Agreement, вече НЕ хвърля Exception, а връща null.
     * В нормалния flow, ако няма Agreement, UserConsentService не се използва.
     **/
//    private Agreement getActiveAgreementOrThrow(User user, String warnMessage, String exceptionMessage) {
//        try {
//            return agreementService.getActiveAgreement();
//        } catch (Exception e) {
//            log.warn(warnMessage, user.getId(), user.getEmail());
//            // Вместо Exception връщаме null, за да позволим вход без Agreement
//            return null;
//        }
//    }

    private Optional<Agreement> getActiveAgreementSafely(User user, String warnMessage) {
        Optional<Agreement> activeAgreementOpt = agreementService.getActiveAgreement();

        if (activeAgreementOpt.isEmpty()) {
            log.warn(warnMessage, user.getId(), user.getEmail());
        }
        return activeAgreementOpt;
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
}
