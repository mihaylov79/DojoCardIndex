package cardindex.dojocardindex.EventParticipationRequest.repository;

import cardindex.dojocardindex.EventParticipationRequest.model.EventParticipationRequest;
import org.hibernate.validator.constraints.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventParticipationRequestRepository extends JpaRepository<EventParticipationRequest, UUID> {

}
