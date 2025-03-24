package cardindex.dojocardindex.EventParticipationRequest.repository;

import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.EventParticipationRequest.model.EventParticipationRequest;
import cardindex.dojocardindex.EventParticipationRequest.model.RequestStatus;

import cardindex.dojocardindex.User.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventParticipationRequestRepository extends JpaRepository<EventParticipationRequest, UUID> {

    List<EventParticipationRequest> findByStatus(RequestStatus status);

    List<EventParticipationRequest> findByStatusOrderByEvent(RequestStatus status);

    Optional<EventParticipationRequest> findByUserAndEvent(User user, Event event);

    Optional<List<EventParticipationRequest>> findAllByUserAndEvent(User user, Event event);

    List<EventParticipationRequest> findAllByUser(User user);

    List<EventParticipationRequest> findAllByStatusIsNot(RequestStatus status);
}
