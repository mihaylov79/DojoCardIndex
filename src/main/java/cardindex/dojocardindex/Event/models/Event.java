package cardindex.dojocardindex.Event.models;

import cardindex.dojocardindex.User.models.User;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Builder(toBuilder = true)
@Entity
@Table(name = "events")
@Getter
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_type",nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType type;

    @Column(name = "description",nullable = false)
    private String EventDescription;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String location;

    @Column
    @Enumerated(EnumType.STRING)
    private Requirements requirements;

    @Column
    private boolean closed;

    @ManyToMany(mappedBy = "events",fetch = FetchType.EAGER)
    private Set<User>users = new LinkedHashSet<>();

    @ManyToOne
    @JoinColumn(name = "first_place_winner")
    private User firstPlaceWinner;

    @ManyToOne
    @JoinColumn(name = "second_place_winner")
    private User secondPlaceWinner;

    @ManyToOne
    @JoinColumn(name = "third_place_winner")
    private User thirdPlaceWinner;


    public Event() {

    }

    public Event(UUID id, EventType type, String eventDescription, LocalDate startDate, LocalDate endDate, String location, Requirements requirements, boolean closed, Set<User> users, User firstPlaceWinner, User secondPlaceWinner, User thirdPlaceWinner) {
        this.id = id;
        this.type = type;
        EventDescription = eventDescription;
        this.startDate = startDate;
        this.endDate = endDate;
        this.location = location;
        this.requirements = requirements;
        this.closed = closed;
        this.users = users;
        this.firstPlaceWinner = firstPlaceWinner;
        this.secondPlaceWinner = secondPlaceWinner;
        this.thirdPlaceWinner = thirdPlaceWinner;
    }

    public UUID getId() {
        return id;
    }

    public User getFirstPlaceWinner() {
        return firstPlaceWinner;
    }

    public void setFirstPlaceWinner(User firstPlaceWinner) {
        this.firstPlaceWinner = firstPlaceWinner;
    }

    public User getSecondPlaceWinner() {
        return secondPlaceWinner;
    }

    public void setSecondPlaceWinner(User secondPlaceWinner) {
        this.secondPlaceWinner = secondPlaceWinner;
    }

    public User getThirdPlaceWinner() {
        return thirdPlaceWinner;
    }

    public void setThirdPlaceWinner(User thirdPlaceWinner) {
        this.thirdPlaceWinner = thirdPlaceWinner;
    }

    public String getLocation() {
        return location;
    }

    public LocalDate getStartDate() {
        return startDate;
    }


    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getEventDescription() {
        return EventDescription;
    }


    public LocalDate getEndDate() {
        return endDate;
    }


    public Requirements getRequirements() {
        return requirements;
    }

    public Set<User> getUsers() {
        return users;
    }


    public boolean isClosed() {
        return closed;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(id, event.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
