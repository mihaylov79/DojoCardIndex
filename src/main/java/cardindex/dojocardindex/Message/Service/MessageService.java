package cardindex.dojocardindex.Message.Service;

import cardindex.dojocardindex.Message.Repository.MessageRepository;
import cardindex.dojocardindex.Message.models.Message;
import cardindex.dojocardindex.User.models.RegistrationStatus;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.exceptions.MessageNotFoundException;
import cardindex.dojocardindex.exceptions.UserNotFoundException;
import cardindex.dojocardindex.web.dto.SendMessageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.SortArgumentResolver;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;
    private final SortArgumentResolver sortArgumentResolver;

    @Autowired
    public MessageService(MessageRepository messageRepository, UserService userService, SortArgumentResolver sortArgumentResolver) {
        this.messageRepository = messageRepository;
        this.userService = userService;
        this.sortArgumentResolver = sortArgumentResolver;
    }

    public List<Message> getReceivedMessagesByUser(UUID userId) {
        return messageRepository.findByRecipient_IdAndIsReadFalse(userId, Sort.by(Sort.Order.desc("created")));
    }

    public Message findMessageById(UUID id){
        return messageRepository.findById(id).orElseThrow(()-> new MessageNotFoundException("Съобщение с ид [%s] не беше открито".formatted(id)));
    }

    public void sendMessage(SendMessageRequest sendMessageRequest) {


        String senderEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println(senderEmail);

        User recipient = userService.findUserByEmail(sendMessageRequest.getRecipient());

        if (recipient == null || recipient.getRegistrationStatus() == RegistrationStatus.NOT_REGISTERED) {
            throw new UserNotFoundException("Този потребител не е регистриран");
        }

        if (recipient.getRegistrationStatus() == RegistrationStatus.PENDING) {
            throw new UserNotFoundException("Регистрацията на този потребител е в процес на потвърждение");
        }

        User sender = userService.findUserByEmail(senderEmail);

        Message message = Message.builder()
                .recipient(recipient)
                .sender(sender)
                .content(sendMessageRequest.getContent())
                .isRead(false)
                .created(LocalDateTime.now())
                .build();

        messageRepository.save(message);


    }

    public void replyMessage(UUID messageId, String content) {

        User sender = userService.getCurrentUser();

        Optional<Message> originalMessageOpt = messageRepository.findById(messageId);
        if (originalMessageOpt.isEmpty()) {
            throw new MessageNotFoundException("Съобщението не беше намерено.");
        }

        Message originalMessage = originalMessageOpt.get();

        // Създаваме ново съобщение, което ще бъде отговор на оригиналното съобщение
        Message replyMessage = Message.builder()
                .content(content)
                .sender(sender)
                .recipient(originalMessage.getSender())
                .responseToMessageId(originalMessage.getId())
                .created(LocalDateTime.now())
                .isRead(false)
                .build();

        messageRepository.save(replyMessage);
    }

    public void removeMessage(UUID messageId){
        Optional<Message>receivedMessage = messageRepository.findById(messageId);

        if (receivedMessage.isEmpty()){
            throw new MessageNotFoundException("Съобщениете не беше намерено.");
        }

        Message message = receivedMessage.get();

        message = message.toBuilder().isRead(true).build();

        messageRepository.save(message);
    }


}
