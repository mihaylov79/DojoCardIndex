package cardindex.dojocardindex.UserConsent.repository;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.UserConsent.model.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, UUID> {
    Optional<UserConsent> findByUser(User user);

    List<UserConsent> findAllBySentMailStatusFailed();

    Optional<UserConsent> findByConsentToken(String consentToken);

    List<UserConsent> findAllByPendingTrueOrderByAgreedAtDesc();
}
