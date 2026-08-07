package cardindex.dojocardindex.UserConsentHistory.repository;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.UserConsent.model.UserConsent;
import cardindex.dojocardindex.UserConsentHistory.model.UserConsentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserConsentHistoryRepository extends JpaRepository<UserConsentHistory, UUID> {

    List<UserConsentHistory> findByConsentOrderByActionAtDesc(UserConsent consent);

    List<UserConsentHistory> findByConsent_UserOrderByActionAtDesc(User user);
}
