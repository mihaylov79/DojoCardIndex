package cardindex.dojocardindex.UserConsent.service;

import cardindex.dojocardindex.Agreement.model.Agreement;
import cardindex.dojocardindex.Agreement.service.AgreementService;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.models.UserRole;
import cardindex.dojocardindex.User.models.UserStatus;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.UserConsent.model.CancelInitiator;
import cardindex.dojocardindex.exceptions.*;
import cardindex.dojocardindex.UserConsent.model.MailSendStatus;
import cardindex.dojocardindex.UserConsent.model.UserConsent;
import cardindex.dojocardindex.UserConsent.repository.UserConsentRepository;
import cardindex.dojocardindex.notification.client.NotificationClient;
import cardindex.dojocardindex.web.dto.CancellationConfirmationRequest;
import cardindex.dojocardindex.web.dto.ParentConsentConfirmationRequest;
import cardindex.dojocardindex.web.dto.ParentConsentInvitationRequest;
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
        return getOptConsent(user, activeAgreementOpt.get())
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
                .sentInvitationMailStatus(MailSendStatus.UNNECESSARY)
                .sentConfirmationMailStatus(MailSendStatus.UNNECESSARY)
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
        // Проверка за вече съществуващо съгласие
        Optional<UserConsent> existingConsentOpt = getOptConsent(user, activeAgreement);
        if (existingConsentOpt.isPresent()) {
            // Ако има съгласие (незавършено или завършено), не правим нищо
            return;
        }

        String token = generateSecureToken();
        UserConsent consent = UserConsent.builder()
                .user(user)
                .agreement(activeAgreement)
                .agreedAt(LocalDateTime.now())
                .isMinor(true)
                .parentEmail(user.getContactPersonEmail())
                .consentToken(token)
                .tokenExpiresAt(LocalDateTime.now().plusHours(24))
                .pending(false)
                .finished(false)
                .createdAt(LocalDateTime.now())
                .sentInvitationMailStatus(MailSendStatus.SENT)
                .build();
        repository.save(consent);

        ParentConsentInvitationRequest emailRequest = ParentConsentInvitationRequest.builder()
                .parentEmail(user.getContactPersonEmail())
                .childFirstName(user.getFirstName())
                .childLastName(user.getLastName())
                .agreementTitle(activeAgreement.getTitle())
                .consentLink(baseUrl + "/parent-consent/" + token)
                .build();
        sendParentConsentInvitationEmailWithStatus(consent, emailRequest, user);
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

        consent = consent.toBuilder()
                .parentConsentedAt(LocalDateTime.now())
                .pending(false)
                .pendingReason(null)
                .finished(true)
                .sentConfirmationMailStatus(MailSendStatus.SENT)
                .build();

        repository.save(consent);

        User user = consent.getUser();

        ParentConsentConfirmationRequest confirmationRequest = ParentConsentConfirmationRequest.builder()
                .parentEmail(user.getContactPersonEmail())
                .childFirstName(user.getFirstName())
                .childLastName(user.getLastName())
                .agreementTitle(consent.getAgreement().getTitle())
                .agreementContent(consent.getAgreement().getContent())
                .parentConsentAt(consent.getParentConsentedAt())
                .consentId(consent.getId())
                .build();

        sendParentConsentConfirmationEmailWithStatus(consent, confirmationRequest, user);

        return consent;

//        return repository.save(consent.toBuilder()
//                .parentConsentedAt(LocalDateTime.now())
//                .pending(false)
//                .pendingReason(null)
//                .finished(true)
//                .build());

        
    }
    
    public void regenerateParentConsentToken(User user) {
        Optional<Agreement> activeAgreementOpt = getActiveAgreementSafely(user,
                "[CONSENT] Невъзможно е да се генерира нов токен за родителско съгласие за потребител {} ({}), защото няма активно споразумение (Agreement).");

        if (activeAgreementOpt.isEmpty()){
            return;
        }

        Agreement agreement = activeAgreementOpt.get();


        UserConsent consent = getOptConsent(user,agreement)
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
        
        ParentConsentInvitationRequest emailRequest = ParentConsentInvitationRequest.builder()
                .parentEmail(user.getContactPersonEmail())
                .childFirstName(user.getFirstName())
                .childLastName(user.getLastName())
                .consentLink(baseUrl + "/parent-consent/" + newToken)
                .agreementTitle(agreement.getTitle())
                .build();
        sendParentConsentInvitationEmailWithStatus(consent, emailRequest, user);
    }
    
    @Transactional
    public void refuseConsent(User user) {
        User deactivated = user.toBuilder()
                .status(UserStatus.INACTIVE)
                .build();
        userService.saveUser(deactivated);
    }

    @Transactional
    public UserConsent cancelConsentByUser(){

        User loggedUser = userService.getCurrentUser();
        
        Optional<Agreement> optActiveAgreement = agreementService.getActiveAgreement();

        if (optActiveAgreement.isEmpty()){
            throw new AgreementNotFoundException("Няма намерено активно споразумение!");
        }
        Agreement activeAgreement = optActiveAgreement.get();

        Optional<UserConsent> optConsent = getOptConsent(loggedUser, activeAgreement);

        if (optConsent.isEmpty()){
            throw new UserConsentNotFoundException("Няма съгласие на този потребител за Активното споразумение");
        }

        UserConsent consent = optConsent.get();

        if (consent.isCanceled()){
            throw  new RuntimeException("Това съгласие е вече ОТТЕГЛЕНО!");
        }

        if (consent.isMinor()){
            throw new RuntimeException("Съгласието за малолетни потребители може да бъде оттеглено от родител(настойник). Моля свържете се с ръководството на клуба!");
        }

        if (consent.isPending()){
            throw new RuntimeException("Това съгласие е служебно одобрено от Администратор и за да бъде оттеглено трябва да се свържете се с ръководството на клуба");
        }

        if (!consent.isFinished()){
            throw new RuntimeException("Съгласието не е окончателно потвърдено, и не може да бъде оттеглено на този етап! Моля свържете се с ръководството на клуба");
        }

//        if (!loggedUser.getId().equals(consent.getUser().getId())) {
//            throw new RuntimeException("Не можете да извършите това действие за друг потребител!");
//        }

        consent = consent.toBuilder()
                .canceled(true)
                .canceledAt(LocalDateTime.now())
                .canceledBy(loggedUser)
                .cancelInitiatedBy(CancelInitiator.USER)
                .cancellationConfirmationMailStatus(MailSendStatus.SENT)
                .build();

        repository.save(consent);

        loggedUser = loggedUser.toBuilder()
                .status(UserStatus.INACTIVE)
                .build();
        userService.saveUser(loggedUser);
        
        CancellationConfirmationRequest emailRequest = CancellationConfirmationRequest.builder()
                .recipientMail(loggedUser.getEmail())
                .userFirstName(loggedUser.getFirstName())
                .userLastName(loggedUser.getLastName())
                .agreementTitle(consent.getAgreement().getTitle())
                .cancelledAt(consent.getCanceledAt())
                .consentId(consent.getId())
                .build();
        
        sendCancelConsentConfirmationEmailWithStatus(consent, emailRequest, loggedUser);
        
        return consent;
    }

    @Transactional
    public UserConsent cancelConsentByParent(UUID consentId, CancelInitiator consentInitiator){

       UserConsent consent = getConsentById(consentId);

       if (!consent.isMinor()){
           throw  new RuntimeException("Родитеското съгасие е необходимо само за непълнолетни потребитеи! Този отказ е неприложим за пълнолетни потребители!");
       }

        cancelConsentByAdmin(consentId, consentInitiator);
        //TODO да довърша метода

        CancellationConfirmationRequest request = CancellationConfirmationRequest.builder()
                .recipientMail(consent.getParentEmail())
                .userFirstName(consent.getUser().getFirstName())
                .userLastName(consent.getUser().getLastName())
                .agreementTitle(consent.getAgreement().getTitle())
                .consentId(consent.getId())
                .build();

        sendCancelConsentConfirmationEmailWithStatus(consent, request, consent.getUser());

        return consent;
    }


    private UserConsent cancelConsentByAdmin(UUID consentId, CancelInitiator cancelInitiator){

        //TODO Да преценя какви проверки да заложа за този метод - да има ли finished и pending
        User loggedUser = userService.getCurrentUser();

        if (loggedUser.getRole() != UserRole.ADMIN && loggedUser.getRole() != UserRole.TRAINER) {
            throw new RuntimeException("За да извършите това действие е необходимо да имате администраторски права!");
        }

        UserConsent consent = getConsentById(consentId);

        if(consent.isCanceled()){
            throw new RuntimeException("Това съгласие е вече ОТТЕГЛЕНО");
        }

        consent = consent.toBuilder()
                .canceled(true)
                .canceledAt(LocalDateTime.now())
                .canceledBy(loggedUser)
                .cancellationConfirmationMailStatus(MailSendStatus.SENT)
                .cancelInitiatedBy(cancelInitiator)
                .build();

        repository.save(consent);

        User consentUser = consent.getUser();
        consentUser =  consentUser.toBuilder()
                .status(UserStatus.INACTIVE)
                .build();
        userService.saveUser(consentUser);

        return consent;


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
                .sentInvitationMailStatus(MailSendStatus.UNNECESSARY)
                .sentConfirmationMailStatus(MailSendStatus.UNNECESSARY)
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


    public List<UserConsent> getConsentsInvitationFailedMails() {

        return repository.findAllBySentInvitationMailStatus(MailSendStatus.INVITATION_FAILED);
    }

    public List<UserConsent> getConsentsConfirmationFailedMails() {

        return repository.findAllBySentConfirmationMailStatus(MailSendStatus.CONFIRMATION_FAILED);
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
        Optional<Agreement> activeAgreementOpt = agreementService.getActiveAgreement();
        if (activeAgreementOpt.isEmpty()) return false;
        return repository.findByUserAndAgreement(user, activeAgreementOpt.get())
                .map(c -> c.getParentConsentedAt() != null)
                .orElse(false);
    }

    public boolean isParentConsentTokenExpired(User user) {
        Optional<Agreement> activeAgreementOpt = agreementService.getActiveAgreement();
        if (activeAgreementOpt.isEmpty()) return false;
        return repository.findByUserAndAgreement(user, activeAgreementOpt.get())
                .map(c -> !c.isTokenValid())
                .orElse(false);
    }

    public boolean isWaitingForParentConsent(User user) {
        Optional<Agreement> activeAgreementOpt = agreementService.getActiveAgreement();
        if (activeAgreementOpt.isPresent()) {
            Optional<UserConsent> consentOpt = getOptConsent(user, activeAgreementOpt.get());
            if (consentOpt.isPresent()) {
                UserConsent consent = consentOpt.get();
                // Ако е pending (от админ), не чака родител
                if (consent.isPending()) return false;
                return consent.isMinor() &&
                        consent.getConsentToken() != null &&
                        consent.getTokenExpiresAt() != null &&
                        consent.getTokenExpiresAt().isAfter(LocalDateTime.now()) &&
                        !consent.isFinished() &&
                        consent.getParentConsentedAt() == null;
            }
        }
        return false;
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

    private void sendParentConsentInvitationEmailWithStatus(UserConsent consent, ParentConsentInvitationRequest emailRequest, User user) {
        try {
            notificationClient.sendParentConsentInvitationEmail(emailRequest);
        } catch (Exception e) {
            consent = consent.toBuilder()
                    .sentInvitationMailStatus(MailSendStatus.INVITATION_FAILED)
                    .build();
            repository.save(consent);
            log.error("Грешка при изпращане на мейл (INVITATION) за родителско съгласие за consent {} user {}: {}", consent.getId(), user.getId(), e.getMessage(), e);
        }
    }

    private void sendParentConsentConfirmationEmailWithStatus(UserConsent consent, ParentConsentConfirmationRequest emailRequest, User user) {
        try {
            notificationClient.sendParentConsentConfirmationEmail(emailRequest);
        } catch (Exception e) {
            consent = consent.toBuilder()
                    .sentConfirmationMailStatus(MailSendStatus.CONFIRMATION_FAILED)
                    .build();
            repository.save(consent);
            log.error("Грешка при изпращане на мейл за ПОТВЪРЖДЕНИЕ на родителско съгласие за consent {} user {}: {}", consent.getId(), user.getId(), e.getMessage(), e);
        }
    }

    private void sendCancelConsentConfirmationEmailWithStatus(UserConsent consent, CancellationConfirmationRequest emailRequest, User user) {
        try {
            notificationClient.sendCancelConfirmationEmail(emailRequest);
        } catch (Exception e) {
            consent = consent.toBuilder()
                    .sentConfirmationMailStatus(MailSendStatus.CANCELLATION_FAILED)
                    .build();
            repository.save(consent);
            log.error("Грешка при изпращане на мейл за ОТКАЗ от съгласие за consent {} user {}: {}", consent.getId(), user.getId(), e.getMessage(), e);
        }
    }

    public Optional<UserConsent> getOptConsent(User user, Agreement agreement) {
        return repository.findByUserAndAgreement(user, agreement);
    }

    /**
     * Връща оставащите секунди до изтичане на токена за родителско съгласие,
     * ако има такъв. Ако няма активен токен, връща 0.
     */
    public long getParentConsentTokenSecondsLeft(User user) {
        Optional<Agreement> activeAgreementOpt = agreementService.getActiveAgreement();
        if (activeAgreementOpt.isPresent()) {
            Optional<UserConsent> consentOpt = getOptConsent(user, activeAgreementOpt.get());
            if (consentOpt.isPresent() && consentOpt.get().getTokenExpiresAt() != null) {
                long secondsLeft = java.time.Duration.between(java.time.LocalDateTime.now(), consentOpt.get().getTokenExpiresAt()).getSeconds();
                return Math.max(secondsLeft, 0);
            }
        }
        return 0;
    }
}
