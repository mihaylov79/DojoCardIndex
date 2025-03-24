package cardindex.dojocardindex.Comment.Service;


import cardindex.dojocardindex.Comment.Repository.CommentRepository;
import cardindex.dojocardindex.Comment.models.Comment;
import cardindex.dojocardindex.Post.Service.PostService;
import cardindex.dojocardindex.Post.models.Post;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.models.UserRole;
import cardindex.dojocardindex.web.dto.CreateCommentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostService postService;

    @Autowired
    public CommentService(CommentRepository commentRepository, PostService postService) {
        this.commentRepository = commentRepository;
        this.postService = postService;
    }

    //TODO Да преценя дали ще ползвам boolean removed или директно ще трия когато искам да премахна коментар
    public void addComment(UUID postId, User user, CreateCommentRequest createCommentRequest){
        Post post = postService.getPostById(postId);

        Comment comment = Comment.builder()
                .post(post)
                .commentAuthor(user)
                .content(createCommentRequest.getContent())
                .commented(LocalDateTime.now())
                .build();

        post.addComment(comment);

        commentRepository.save(comment);

    }

    public Comment getCommentById(UUID commentId){

        return commentRepository.findById(commentId).orElseThrow(()-> new RuntimeException("Такъв коментар не е намерен!"));
    }


    @Transactional
    public void deleteComment(UUID commentId, User user){

       Comment comment = getCommentById(commentId);

        if (!user.equals(comment.getCommentAuthor()) && user.getRole()!= UserRole.ADMIN){

            throw new RuntimeException("Само Администратор или авторът на коментара могат да го премахнат");

        }

        Post post = comment.getPost();
        if (post != null){
            post.getComments().remove(comment);
        }

        User commentAuthor = comment.getCommentAuthor();
        if (commentAuthor != null)
            commentAuthor.getComments().remove(comment);

       commentRepository.delete(comment);
    }



    public List<Comment> getCommentsForPost(UUID postId){

        return commentRepository.findByPost_Id(postId);
    }


}
