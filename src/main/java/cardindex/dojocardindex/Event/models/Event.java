package cardindex.dojocardindex.Event.models;

import cardindex.dojocardindex.User.models.User;
import jakarta.persistence.*;
import lombok.Setter;


import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(name = "event_type",nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType type;

    @Setter
    @Column(name = "description",nullable = false)
    private String EventDescription;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column
    @Enumerated(EnumType.STRING)
    private Requirements requirements;

    @ManyToMany(mappedBy = "events")
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

    public Event(UUID uuid, EventType type, String eventDescription, LocalDate startDate, LocalDate endDate, Requirements requirements, Set<User> users) {
        this.uuid = uuid;
        this.type = type;
        EventDescription = eventDescription;
        this.startDate = startDate;
        this.endDate = endDate;
        this.requirements = requirements;
        this.users = users;
    }

    public UUID getUuid() {
        return uuid;
    }

    public User getFirstPlaceWinner() {
        return firstPlaceWinner;
    }

    //TODO: Да добавя CustomExceptions TournamentExceptions
    public void setFirstPlaceWinner(User firstPlaceWinner) {
        if(this.type == EventType.TOURNAMENT) {
            this.firstPlaceWinner = firstPlaceWinner;
        }else {
            throw new IllegalArgumentException("Winner can be set only for TOURNAMENT event");
        }
        if (!users.contains(firstPlaceWinner)) {
            throw new IllegalArgumentException("Winner must be a participant in the event.");
        }
        if (this.firstPlaceWinner != null) {
            this.firstPlaceWinner.setAchievedFirstPlaces(this.firstPlaceWinner.getAchievedFirstPlaces() - 1);
        }
        this.firstPlaceWinner = firstPlaceWinner;
        if (firstPlaceWinner != null) {
            firstPlaceWinner.setAchievedFirstPlaces(firstPlaceWinner.getAchievedFirstPlaces() + 1);
        }

    }

    public User getSecondPlaceWinner() {
        return secondPlaceWinner;
    }

    public void setSecondPlaceWinner(User secondPlaceWinner) {
        if (this.type == EventType.TOURNAMENT) {
            this.secondPlaceWinner = secondPlaceWinner;
        }else {
            throw new IllegalArgumentException("Winner can be set only for TOURNAMENT event");
        }
        if (!users.contains(secondPlaceWinner)) {
            throw new IllegalArgumentException("Winner must be a participant in the event.");
        }
        if (this.secondPlaceWinner != null) {
            this.secondPlaceWinner.setAchievedSecondPlaces(this.secondPlaceWinner.getAchievedSecondPlaces() - 1);
        }
        this.secondPlaceWinner = secondPlaceWinner;
        if (secondPlaceWinner != null) {
            secondPlaceWinner.setAchievedSecondPlaces(secondPlaceWinner.getAchievedSecondPlaces() + 1);
        }

    }

    public User getThirdPlaceWinner() {
        return thirdPlaceWinner;
    }

    public void setThirdPlaceWinner(User thirdPlaceWinner) {
        if (this.type == EventType.TOURNAMENT) {
            this.thirdPlaceWinner = thirdPlaceWinner;
        }else {
            throw new IllegalArgumentException("Winner can be set only for TOURNAMENT event");
        }

        if (!users.contains(thirdPlaceWinner)) {
            throw new IllegalArgumentException("Winner must be a participant in the event.");
        }
        if (this.thirdPlaceWinner != null) {
            this.thirdPlaceWinner.setAchievedThirdPlaces(this.thirdPlaceWinner.getAchievedThirdPlaces() - 1);
        }
        this.thirdPlaceWinner = thirdPlaceWinner;
        if (thirdPlaceWinner != null) {
            thirdPlaceWinner.setAchievedThirdPlaces(thirdPlaceWinner.getAchievedThirdPlaces() + 1);
        }

    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setEventDescription(String eventDescription) {
        EventDescription = eventDescription;
    }

    //    public void setUuid(UUID uuid) {
//        this.uuid = uuid;
//    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getEventDescription() {
        return EventDescription;
    }


    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Requirements getRequirements() {
        return requirements;
    }

    public void setRequirements(Requirements requirements) {
        this.requirements = requirements;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }
}
