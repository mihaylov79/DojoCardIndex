package cardindex.dojocardindex.telegram;


import cardindex.dojocardindex.Comment.models.Comment;
import cardindex.dojocardindex.Post.models.Post;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TelegramNotificationService {

    private final String botToken;
    private final String channelChatId;
    private final boolean notificationsEnabled;
    private final RestTemplate restTemplate;

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    public TelegramNotificationService(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.channel.chat.id}") String channelChatId,
            @Value("${telegram.notifications.enabled:false}") boolean notificationsEnabled) {
        this.botToken = botToken;
        this.channelChatId = channelChatId;
        this.notificationsEnabled = notificationsEnabled;
        this.restTemplate = new RestTemplate();
    }

    public void notifyNewPost(Post post) {
        if (!notificationsEnabled) {
            log.debug("Telegram нотификациите са изключени за публикации");
            return;
        }

        String message = buildPostMessage(post);
        sendMessage(message);

        log.info("Telegram нотификация изпратена за нова публикация: {}", post.getTitle());
    }

    public void notifyNewComment(Comment comment) {
        if (!notificationsEnabled) {
            log.debug("Telegram нотификациите са изключени за коментари");
            return;
        }

        String message = buildCommentMessage(comment);
        sendMessage(message);

        log.info("Telegram нотификация изпратена за коментар към пост: {}", comment.getPost().getTitle());
    }

    private void sendMessage(String text){
        try{
            String url = TELEGRAM_API_URL + botToken + "/sendMessage";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", channelChatId);
            requestBody.put("text", text);
            requestBody.put("parse_mode", "Markdown");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody,headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url,request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Telegram нотификация изпратена успешно");
            }else {
                log.warn("Telegram API върнаа статус: {}",response.getStatusCode());
            }
        } catch (Exception e) {
            log.warn("Грешка при изпращане на Telegram нотификация: {}",e.getMessage());
            log.warn("Моля проверете настройките на Телеграм нотификациите в application.proerties" +
                    ", както дали Бота има администраторски права в канала , " +
                    "има ли права за изпращане на съобщения , " +
                    "както и дали сте свързани към интернет");
        }
    }


    private String buildPostMessage(Post post){
        String title = post.getTitle();
        String firstName = post.getAuthor().getFirstName();
        String lastName = post.getAuthor().getLastName();
        String content = post.getContent();
        String dateTime = post.getCreated().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        return String.format("""
                        🥋 *Нова публикация в Dragon Dojo*
                        
                        📝 *%s*
                        
                        👤 Автор: %s %s
                        📅 %s
                        
                        📄 Съдържание:
                        ----------
                        %s
                        ----------
                        
                        ℹ️ За повече информация и коментари посетете секция Публикации в сайта на Доджото""",
                title,
                firstName,
                lastName,
                dateTime,
                content
                );
    }
    private String buildCommentMessage(Comment comment){
        String post = comment.getPost().getTitle();
        String firstName = comment.getCommentAuthor().getFirstName();
        String lastName = comment.getCommentAuthor().getLastName();
        String content = comment.getContent();
        String dateTime = comment.getCommented().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));

        return String.format("""
                        💬 *Нов коментар в Dragon Dojo*
                        
                        👤 %s %s коментира пост:
                        📝 *%s*
                        
                        📄 Коментар:
                        ----------
                        %s
                        ----------
                        📅 %s
                        
                        ℹ️ За повече информация и коментари посетете секция Публикации в сайта на Доджото""",
                firstName,
                lastName,
                post,
                content,
                dateTime
                );
    }


    public boolean isAvailable() {
        return notificationsEnabled
                && botToken != null
                && !botToken.isEmpty()
                && channelChatId != null
                && !channelChatId.isEmpty();
    }


}
