package cardindex.dojocardindex.Agreement.service;

import cardindex.dojocardindex.Agreement.model.Agreement;
import cardindex.dojocardindex.Agreement.repository.AgreementRepository;
import cardindex.dojocardindex.web.dto.AgreementRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AgreementService {

    private final AgreementRepository repository;

    public AgreementService(AgreementRepository repository) {
        this.repository = repository;
    }

    public Agreement getActiveAgreement(){
        return repository.findByActiveTrue().orElse(null);
    }

    public Agreement getAgreementById(UUID id){

        return repository.findById(id).orElseThrow(() ->
                new RuntimeException("Споразумение с идентификация [%s] не е намерено!".formatted(id)));
    }

    @Transactional
    public Agreement publishNewAgreement(UUID agreementId){

        Agreement toBePublished = getAgreementById(agreementId);

        if (toBePublished.isActive()) {
            throw new RuntimeException("Споразумението вече е активно!");
        }

        repository.findByActiveTrue().ifPresent(old -> {
            Agreement deactivated = old.toBuilder()
                    .active(false)
                    .build();

            repository.save(deactivated);
        });

        toBePublished = toBePublished.toBuilder()
                .active(true)
                .build();

        return repository.save(toBePublished);
    }

    public Agreement createAgreement(AgreementRequest request){

        Agreement newAgreement = Agreement.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .createdAt(LocalDateTime.now())
                .active(false)
                .build();

        return repository.save(newAgreement);
    }
}
