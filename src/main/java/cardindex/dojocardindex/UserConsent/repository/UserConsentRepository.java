package cardindex.dojocardindex.UserConsent.repository;

import cardindex.dojocardindex.Agreement.model.Agreement;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.UserConsent.model.MailSendStatus;
import cardindex.dojocardindex.UserConsent.model.UserConsent;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {

    Optional<UserConsent> findByConsentToken(String consentToken);

    List<UserConsent> findAllByPendingTrueOrderByAgreedAtDesc();

    List<UserConsent> findAllByFinishedFalseAndPendingFalseOrderByCreatedAtDesc();

    List<UserConsent> findAllByOrderByCreatedAtDesc(Sort sort);

    Optional<UserConsent> findByUserAndAgreement(User user, Agreement activeAgreement);

    List<UserConsent> findAllBySentInvitationMailStatusInOrSentInvitationMailStatusIsNull(List<MailSendStatus> statuses);

    List<UserConsent> findAllBySentConfirmationMailStatusInOrSentConfirmationMailStatusIsNull(List<MailSendStatus> statuses);
}
