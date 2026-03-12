package cardindex.dojocardindex.Agreement.repository;

import cardindex.dojocardindex.Agreement.model.Agreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, UUID> {
    Optional<Agreement> findByActiveTrue();
}
