package cardindex.dojocardindex.UserConsentHistory.service;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.UserConsent.model.UserConsent;
import cardindex.dojocardindex.UserConsentHistory.model.ConsentHistoryAction;
import cardindex.dojocardindex.UserConsentHistory.model.UserConsentHistory;
import cardindex.dojocardindex.UserConsentHistory.repository.UserConsentHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public void saveConsentHistory(UserConsentHistory history) {
        historyRepository.save(history);
    }
}
