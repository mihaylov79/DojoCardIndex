package cardindex.dojocardindex.Message.models;

import cardindex.dojocardindex.User.models.User;
import jakarta.persistence.*;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder(toBuilder = true)
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column
    private String content;

    @Column(nullable = false)
    private LocalDateTime created;

    @Column
    private UUID responseToMessageId;

    @Column(nullable = false)
    private boolean isRead;


    public Message() {
    }

    public Message(UUID id, User recipient, User sender, String content, LocalDateTime created, UUID responseToMessageId, boolean isRead) {
        this.id = id;
        this.recipient = recipient;
        this.sender = sender;
        this.content = content;
        this.created = created;
        this.responseToMessageId = responseToMessageId;
        this.isRead = isRead;
    }

    public UUID getId() {
        return id;
    }

    public User getRecipient() {
        return recipient;
    }

    public User getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public UUID getResponseToMessageId() {
        return responseToMessageId;
    }

    public boolean isRead() {
        return isRead;
    }
}
