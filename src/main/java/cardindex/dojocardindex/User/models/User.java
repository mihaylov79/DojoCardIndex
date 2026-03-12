package cardindex.dojocardindex.User.models;

import cardindex.dojocardindex.Comment.models.Comment;
import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.EventParticipationRequest.model.EventParticipationRequest;
import cardindex.dojocardindex.ForgottenPasswordToken.models.ForgottenPasswordToken;
import cardindex.dojocardindex.Post.models.Post;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;

@Table(name = "users")
@Entity
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column
    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Column
    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column
    @Enumerated(EnumType.STRING)
    private RegistrationStatus registrationStatus;

    @Column(name = "firtst_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column
    @Size(max = 15)
    private String userPhone;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "birth_date")
    private LocalDate birthDate;


    @Column(name = "reached_degree")
    @Enumerated(EnumType.STRING)
    private Degree reachedDegree;

    @Column(columnDefinition = "TEXT", length = 500)
    private String interests;

    @Column(name = "age_group")
    @Enumerated(EnumType.STRING)
    private AgeGroup ageGroup;

    @Column(name = "is_competitor")
    private boolean isCompetitor;

    @Column
    private double height;

    @Column
    private double weight;

    @Column(name = "medical_exams_passed")
    private LocalDate medicalExamsPassed;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "contact_person_phone")
    private String contactPersonPhone;

    @Column(name = "contact_person_email")
    private String contactPersonEmail;

    @Column(name = "ahieved_first_places")
    private int achievedFirstPlaces;
    @Column(name = "ahieved_second_places")
    private int achievedSecondPlaces;
    @Column(name = "ahieved_third_places")
    private int achievedThirdPlaces;

    @Column
    private int rating;

    @OneToOne(mappedBy = "user",orphanRemoval = true, cascade = CascadeType.ALL)
    private ForgottenPasswordToken resetToken;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "users_events",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    private Set<Event>events = new LinkedHashSet<>();

    @OneToMany(mappedBy = "commentAuthor")
    private List<Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "author")
    private List<Post> posts = new ArrayList<>();

    @OneToMany(mappedBy = "user")
    private List<EventParticipationRequest> requests = new ArrayList<>();

    @OneToMany(mappedBy = "processedBy")
    private Set<EventParticipationRequest> processedRequests = new LinkedHashSet<>();

    public void setResetToken(ForgottenPasswordToken resetToken) {
        this.resetToken = resetToken;
    }

    public void setRegistrationStatus(RegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public void setAchievedFirstPlaces(int achievedFirstPlaces) {
        this.achievedFirstPlaces = achievedFirstPlaces;
    }

    public void setAchievedSecondPlaces(int achievedSecondPlaces) {
        this.achievedSecondPlaces = achievedSecondPlaces;
    }

    public void setAchievedThirdPlaces(int achievedThirdPlaces) {
        this.achievedThirdPlaces = achievedThirdPlaces;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public void setEvents(Set<Event> events) {
        this.events = events;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }

    public List<Post> getPosts() {
        return posts;
    }

    public List<EventParticipationRequest> getRequests() {
        return requests;
    }

    public Set<EventParticipationRequest> getProcessedRequests() {
        return processedRequests;
    }


    public void removeCommentsByPost(Post post) {
        comments.removeIf(comment -> comment.getPost().equals(post));
        log.warn("Премахване на коментарите от потребител с ID: {} за пост с ID: {}", id, post.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(id, user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
