package cardindex.dojocardindex.Event.repository;

import cardindex.dojocardindex.Event.models.Event;
import org.springframework.data.domain.Limit;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findAllByClosedOrderByStartDate(boolean closed);

    List<Event> findAllByStartDateBeforeAndClosed(LocalDate startDateBefore, boolean closed, Limit limit);

    List<Event> findAllByStartDateAfterAndClosed(LocalDate startDateAfter, boolean closed, Limit limit, Sort sort);
}
