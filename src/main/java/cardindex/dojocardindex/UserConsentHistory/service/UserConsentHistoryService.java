package cardindex.dojocardindex.UserConsentHistory.service;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.UserConsent.model.UserConsent;
import cardindex.dojocardindex.UserConsentHistory.model.ConsentHistoryAction;
import cardindex.dojocardindex.UserConsentHistory.model.UserConsentHistory;
import cardindex.dojocardindex.UserConsentHistory.repository.UserConsentHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserConsentHistoryService {

    private final UserConsentHistoryRepository historyRepository;

    @Autowired
    public UserConsentHistoryService(UserConsentHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public void log(UserConsent consent, ConsentHistoryAction action, User actionBy, String reason) {
        UserConsentHistory history = UserConsentHistory.builder()
                .consent(consent)
                .action(action)
                .actionAt(java.time.LocalDateTime.now())
                .actionBy(actionBy)
                .reason(reason)
                .build();

        historyRepository.save(history);
    }

    //TODO Да видя дали изобщо ще има нужда от метод с notes -
    // възможно е ако не се ползват да премахна полето от модела

    public void log(UserConsent consent, ConsentHistoryAction action, User actionBy, String reason, String notes) {
        UserConsentHistory history = UserConsentHistory.builder()
                .consent(consent)
                .action(action)
                .actionAt(java.time.LocalDateTime.now())
                .actionBy(actionBy)
                .reason(reason)
                .notes(notes)
                .build();

        historyRepository.save(history);
    }

    public List<UserConsentHistory> getHistoryForConsent(UserConsent consent) {
        return historyRepository.findByConsentOrderByActionAtDesc(consent);
    }

    public List<UserConsentHistory> getHistoryForUser(User user) {
        return historyRepository.findByConsent_UserOrderByActionAtDesc(user);
    }

    public Map<String, List<UserConsentHistory>> getHistoryForUserGroupedByAgreementTitle(User user) {
        return getHistoryForUser(user).stream()
                .collect(Collectors.groupingBy(
                        history -> {
                            if (history.getConsent() == null || history.getConsent().getAgreement() == null || history.getConsent().getAgreement().getTitle() == null) {
                                return "Без заглавие";
                            }
                            return history.getConsent().getAgreement().getTitle();
                        },
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    public void saveConsentHistory(UserConsentHistory history) {
        historyRepository.save(history);
    }
}
