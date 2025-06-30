package cardindex.dojocardindex.Utils;

import cardindex.dojocardindex.Message.Service.MessageService;
import cardindex.dojocardindex.Post.Service.PostService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class CleanUpService {

    private final MessageService messageService;
    private final PostService postService;

    public CleanUpService(MessageService messageService, PostService postService) {
        this.messageService = messageService;
        this.postService = postService;
    }

//    @Scheduled(fixedDelay = 60000)
    @Scheduled(cron = "0 0 0 1 */3 *")
    public void deleteOldRecords(){
        LocalDateTime sixMonthsAgo = LocalDateTime.now().minusMonths(6);

        postService.deleteOldPosts(sixMonthsAgo);

        int messagesCount = messageService.deleteOldMessages(sixMonthsAgo);
        log.warn("Изтрити съобщения създадени преди повече от 6 месеца : {}",messagesCount);
    }
}
