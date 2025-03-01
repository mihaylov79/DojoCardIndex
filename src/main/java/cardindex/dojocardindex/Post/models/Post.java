package cardindex.dojocardindex.Post.models;

import cardindex.dojocardindex.Comment.models.Comment;
import cardindex.dojocardindex.User.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Post {

   @Id
   @GeneratedValue(strategy = GenerationType.UUID)
    private UUID Id;

   @ManyToOne(fetch = FetchType.EAGER)
   @JoinColumn(name = "autor_id")
   private User author;

   @Column
   private LocalDateTime created;

   @Column
   private String title;

   @Column
   private String content;

   @Column
   private boolean isRead;

   @OneToMany(mappedBy = "post")
   private List<Comment> comments = new ArrayList<>();




}
