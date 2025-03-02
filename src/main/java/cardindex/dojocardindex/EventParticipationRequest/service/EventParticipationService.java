package cardindex.dojocardindex.EventParticipationRequest.service;

import cardindex.dojocardindex.EventParticipationRequest.repository.EventParticipationRequestRepository;
import cardindex.dojocardindex.User.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EventParticipationService {

    private final EventParticipationRequestRepository eventParticipationRequestRepository;
    private final UserService userService;

    @Autowired
    public EventParticipationService(EventParticipationRequestRepository eventParticipationRequestRepository, UserService userService) {
        this.eventParticipationRequestRepository = eventParticipationRequestRepository;
        this.userService = userService;
    }



}
