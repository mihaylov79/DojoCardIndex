package cardindex.dojocardindex.Event.models;

import cardindex.dojocardindex.User.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Builder(toBuilder = true)
@Entity
@Table(name = "events")
@Getter
@AllArgsConstructor
@NoArgsConstructor
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

    @Column
    private boolean result;

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


    public void setFirstPlaceWinner(User firstPlaceWinner) {
        this.firstPlaceWinner = firstPlaceWinner;
    }
    public void setSecondPlaceWinner(User secondPlaceWinner) {
        this.secondPlaceWinner = secondPlaceWinner;
    }
    public void setThirdPlaceWinner(User thirdPlaceWinner) {
        this.thirdPlaceWinner = thirdPlaceWinner;
    }


    public void setType(EventType type) {
        this.type = type;
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
