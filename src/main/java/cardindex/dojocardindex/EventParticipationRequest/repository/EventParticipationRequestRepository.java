package cardindex.dojocardindex.EventParticipationRequest.repository;

import cardindex.dojocardindex.EventParticipationRequest.model.EventParticipationRequest;
import cardindex.dojocardindex.EventParticipationRequest.model.RequestStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EventParticipationRequestRepository extends JpaRepository<EventParticipationRequest, UUID> {

    List<EventParticipationRequest> findByStatus(RequestStatus status);

    List<EventParticipationRequest> findByStatusOrderByEvent(RequestStatus status);
}
