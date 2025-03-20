package cardindex.dojocardindex.User.models;

import cardindex.dojocardindex.Comment.models.Comment;
import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.EventParticipationRequest.model.EventParticipationRequest;
import cardindex.dojocardindex.Post.models.Post;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;

@Table(name = "users")
@Entity
@Getter
@Builder(toBuilder = true)
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
    @Size(min = 10, max = 15)
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
    private int height;

    @Column
    private int weight;

    @Column(name = "medical_exams_passed")
    private LocalDate medicalExamsPassed;

    @Column(name = "contact_person")
    private String contactPerson;

    @Column(name = "contact_person_phone")
    private String contactPersonPhone;

    @Column(name = "ahieved_first_places")
    private int achievedFirstPlaces;
    @Column(name = "ahieved_second_places")
    private int achievedSecondPlaces;
    @Column(name = "ahieved_third_places")
    private int achievedThirdPlaces;

    @Column
    private int rating;

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
    private List<EventParticipationRequest>requests = new ArrayList<>();

    @OneToMany(mappedBy = "processedBy")
    private Set<EventParticipationRequest> processedRequests = new LinkedHashSet<>();

    public User() {

    }


    public User(UUID id, String email, String password, UserRole role, UserStatus status, RegistrationStatus registrationStatus, String firstName, String lastName, String userPhone, String profilePicture, LocalDate birthDate, Degree reachedDegree, String interests, AgeGroup ageGroup, boolean isCompetitor, int height, int weight, LocalDate medicalExamsPassed, String contactPerson, String contactPersonPhone, int achievedFirstPlaces, int achievedSecondPlaces, int achievedThirdPlaces, int rating, Set<Event> events, List<Comment> comments, List<Post> posts, List<EventParticipationRequest> requests, Set<EventParticipationRequest> processedRequests) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.status = status;
        this.registrationStatus = registrationStatus;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userPhone = userPhone;
        this.profilePicture = profilePicture;
        this.birthDate = birthDate;
        this.reachedDegree = reachedDegree;
        this.interests = interests;
        this.ageGroup = ageGroup;
        this.isCompetitor = isCompetitor;
        this.height = height;
        this.weight = weight;
        this.medicalExamsPassed = medicalExamsPassed;
        this.contactPerson = contactPerson;
        this.contactPersonPhone = contactPersonPhone;
        this.achievedFirstPlaces = achievedFirstPlaces;
        this.achievedSecondPlaces = achievedSecondPlaces;
        this.achievedThirdPlaces = achievedThirdPlaces;
        this.rating = rating;
        this.events = events;
        this.comments = comments;
        this.posts = posts;
        this.requests = requests;
        this.processedRequests = processedRequests;
    }

    public UUID getId() {
        return id;
    }


    public String getEmail() {
        return email;
    }



    public String getPassword() {
        return password;
    }


    public UserRole getRole() {
        return role;
    }



    public UserStatus getStatus() {
        return status;
    }



    public RegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(RegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public String getFirstName() {
        return firstName;
    }


    public String getLastName() {
        return lastName;
    }


    public String getUserPhone() {
        return userPhone;
    }


    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }


    public Degree getReachedDegree() {
        return reachedDegree;
    }


    public String getInterests() {
        return interests;
    }

    public void setInterests(String interests) {
        this.interests = interests;
    }

    public AgeGroup getAgeGroup() {
        return ageGroup;
    }


    public boolean getIsCompetitor() {
        return isCompetitor;
    }

//    public void setCompetitor(boolean competitor) {
//        isCompetitor = competitor;
//    }

    public int getHeight() {
        return height;
    }


    public int getWeight() {
        return weight;
    }


    public LocalDate getMedicalExamsPassed() {
        return medicalExamsPassed;
    }


    public String getContactPerson() {
        return contactPerson;
    }


    public String getContactPersonPhone() {
        return contactPersonPhone;
    }

    public int getAchievedFirstPlaces() {
        return achievedFirstPlaces;
    }

    public void setAchievedFirstPlaces(int achievedFirstPlaces) {
        this.achievedFirstPlaces = achievedFirstPlaces;
    }

    public int getAchievedSecondPlaces() {
        return achievedSecondPlaces;
    }

    public void setAchievedSecondPlaces(int achievedSecondPlaces) {
        this.achievedSecondPlaces = achievedSecondPlaces;
    }

    public int getAchievedThirdPlaces() {
        return achievedThirdPlaces;
    }

    public void setAchievedThirdPlaces(int achievedThirdPlaces) {
        this.achievedThirdPlaces = achievedThirdPlaces;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public Set<Event> getEvents() {
        return events;
    }

    public void setEvents(Set<Event> events) {
        this.events = events;
    }

    public List<Comment> getComments() {
        return comments;
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


