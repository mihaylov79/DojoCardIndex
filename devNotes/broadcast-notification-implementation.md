# Broadcast Notification Implementation Guide

## 📋 Съдържание
1. [Общ преглед](#общ-преглед)
2. [Архитектура](#архитектура)
3. [Промени в mail-svc](#промени-в-mail-svc)
4. [Промени в DojoCardIndex](#промени-в-dojocardindex)
5. [Поток на данните](#поток-на-данните)
6. [Предимства](#предимства)
7. [Алтернативни решения](#алтернативни-решения)

---

## 🎯 Общ преглед

### Проблем
При създаване на нов Post, ако искаме да уведомим всички потребители, текущият подход изисква:
- **N индивидуални HTTP заявки** (по една за всеки потребител)
- **N SMTP заявки** за изпращане на мейли
- Високо натоварване на mail-svc при > 50 потребители
- Риск от rate limiting и timeout

### Решение
**Batch API Endpoint** - един endpoint в mail-svc, който приема broadcast заявка и се грижи за изпращането до всички потребители:
- **1 HTTP заявка** от DojoCardIndex → mail-svc
- mail-svc вътрешно обработва изпращането до N потребители
- По-добър контрол върху rate limiting
- Възможност за throttling и batching

### Сценарий на използване
Когато се създаде нов Post в DojoCardIndex:
1. Post се запазва в базата данни
2. Изпраща се broadcast notification към mail-svc
3. mail-svc намира всички потребители с активирани известия
4. mail-svc изпраща email до всеки потребител (с throttling)

---

## 🏗️ Архитектура

```
┌─────────────────────────────────────────────────────────────────┐
│                        DojoCardIndex                            │
│                                                                 │
│  ┌──────────────┐      ┌────────────────┐                      │
│  │ PostService  │─────>│NotificationSvc │                      │
│  │              │      │                │                      │
│  │createNewPost()│      │broadcastNew... │                     │
│  └──────────────┘      └───────┬────────┘                      │
│                                │                                │
│                                │ Feign Client                   │
│                                │ POST /broadcast                │
└────────────────────────────────┼────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────┐
│                          mail-svc                               │
│                                                                 │
│  ┌────────────────────┐      ┌──────────────────┐              │
│  │NotificationCtrl    │─────>│NotificationSvc   │              │
│  │                    │      │                  │              │
│  │POST /broadcast     │      │broadcastAsync()  │              │
│  └────────────────────┘      └────────┬─────────┘              │
│                                       │                         │
│                                       ├─> Find enabled users    │
│                                       ├─> Filter excludeUserIds │
│                                       ├─> For each user:        │
│                                       │   ├─> Save notification │
│                                       │   └─> Send email        │
│                                       └─> Throttling (10/sec)   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📨 Промени в mail-svc

### 1️⃣ Нов DTO - BroadcastNotificationRequest

**Файл:** `dto/BroadcastNotificationRequest.java`

```java
package cardindex.mailsvc.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastNotificationRequest {
    
    /**
     * Заглавие на нотификацията (напр. "Нова публикация: Java Tips")
     */
    private String title;
    
    /**
     * Съдържание на нотификацията
     */
    private String content;
    
    /**
     * Списък с потребители, които да бъдат изключени от broadcast
     * (напр. автора на поста)
     * Optional - може да е празен
     */
    @Builder.Default
    private List<UUID> excludeUserIds = List.of();
}
```

---

### 2️⃣ Нов Controller Endpoint

**Файл:** `controller/NotificationController.java`

```java
package cardindex.mailsvc.notification.controller;

import cardindex.mailsvc.notification.dto.BroadcastNotificationRequest;
import cardindex.mailsvc.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // ...existing endpoints...

    /**
     * Broadcast notification endpoint
     * Изпраща нотификация до всички потребители с активирани известия
     * 
     * @param request BroadcastNotificationRequest с title, content и excludeUserIds
     * @return 202 Accepted - обработката е асинхронна
     */
    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcastNotification(
            @RequestBody BroadcastNotificationRequest request) {
        
        log.info("Получена broadcast заявка с title: [{}]", request.getTitle());
        
        // Валидация
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            log.warn("Broadcast заявка отхвърлена - липсва title");
            return ResponseEntity.badRequest().build();
        }
        
        if (request.getContent() == null || request.getContent().isBlank()) {
            log.warn("Broadcast заявка отхвърлена - липсва content");
            return ResponseEntity.badRequest().build();
        }
        
        // Стартира асинхронен процес
        notificationService.broadcastNotificationAsync(request);
        
        // Връща 202 Accepted веднага (обработката продължава в background)
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
```

---

### 3️⃣ Service Logic

**Файл:** `service/NotificationService.java`

**Добави следните методи:**

```java
package cardindex.mailsvc.notification.service;

import cardindex.mailsvc.notification.dto.BroadcastNotificationRequest;
import cardindex.mailsvc.notification.entity.Notification;
import cardindex.mailsvc.notification.entity.NotificationPreference;
import cardindex.mailsvc.notification.repository.NotificationRepository;
import cardindex.mailsvc.notification.repository.NotificationPreferenceRepository;
import cardindex.mailsvc.email.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final EmailService emailService;

    @Autowired
    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationPreferenceRepository preferenceRepository,
            EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.preferenceRepository = preferenceRepository;
        this.emailService = emailService;
    }

    // ...existing methods...

    /**
     * Изпраща broadcast нотификация до всички потребители с enabled notifications
     * 
     * @param request BroadcastNotificationRequest
     */
    @Async
    public void broadcastNotificationAsync(BroadcastNotificationRequest request) {
        log.info("Стартиране на broadcast процес за: [{}]", request.getTitle());
        
        try {
            // 1. Намери всички потребители с включени нотификации
            List<NotificationPreference> enabledPreferences = 
                preferenceRepository.findAllByEnabledIsTrue();
            
            log.info("Намерени [{}] потребители с включени нотификации", 
                enabledPreferences.size());
            
            // 2. Филтрирай excludeUserIds
            List<NotificationPreference> targetUsers = enabledPreferences.stream()
                .filter(pref -> !request.getExcludeUserIds().contains(pref.getRecipientId()))
                .toList();
            
            log.info("След филтриране: [{}] потребители за broadcast", targetUsers.size());
            
            // 3. Изпрати на батчове с throttling
            broadcastInBatches(targetUsers, request);
            
            log.info("Broadcast процес завърши успешно. Изпратени [{}] нотификации", 
                targetUsers.size());
            
        } catch (Exception e) {
            log.error("Грешка при broadcast процес", e);
        }
    }

    /**
     * Изпраща нотификации на батчове с throttling
     * Предпазва от rate limiting на SMTP сървъра
     * 
     * @param targetUsers Списък с потребители
     * @param request Broadcast заявка
     */
    private void broadcastInBatches(
            List<NotificationPreference> targetUsers, 
            BroadcastNotificationRequest request) {
        
        int batchSize = 10; // 10 мейла на батч
        int delayBetweenBatches = 1000; // 1 секунда между батчовете
        
        for (int i = 0; i < targetUsers.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, targetUsers.size());
            List<NotificationPreference> batch = targetUsers.subList(i, endIndex);
            
            log.info("Обработка на батч [{}/{}] - потребители [{}-{}]", 
                (i / batchSize) + 1, 
                (targetUsers.size() + batchSize - 1) / batchSize,
                i + 1, 
                endIndex);
            
            // Изпрати мейлове в текущия батч
            batch.forEach(pref -> sendBroadcastEmail(pref, request));
            
            // Изчакай между батчовете (ако не е последния)
            if (endIndex < targetUsers.size()) {
                try {
                    Thread.sleep(delayBetweenBatches);
                } catch (InterruptedException e) {
                    log.warn("Broadcast процес прекъснат", e);
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Изпраща broadcast email на един потребител
     * 
     * @param preference NotificationPreference на потребителя
     * @param request Broadcast заявка
     */
    private void sendBroadcastEmail(
            NotificationPreference preference, 
            BroadcastNotificationRequest request) {
        
        try {
            // Създай notification record за history
            Notification notification = Notification.builder()
                .recipientId(preference.getRecipientId())
                .title(request.getTitle())
                .content(request.getContent())
                .timestamp(LocalDateTime.now())
                .build();
            
            notificationRepository.save(notification);
            
            // Изпрати email
            emailService.sendEmail(
                preference.getInfo(), // email address
                request.getTitle(),
                request.getContent()
            );
            
            log.debug("Broadcast email изпратен успешно до [{}]", 
                preference.getRecipientId());
            
        } catch (Exception e) {
            log.error("Грешка при изпращане на broadcast до потребител [{}]", 
                preference.getRecipientId(), e);
            // Продължава с останалите потребители
        }
    }
}
```

---

### 4️⃣ Repository Method

**Файл:** `repository/NotificationPreferenceRepository.java`

**Добави метод:**

```java
package cardindex.mailsvc.notification.repository;

import cardindex.mailsvc.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    
    // ...existing methods...
    
    /**
     * Намери всички потребители с включени нотификации
     * 
     * @return Списък с NotificationPreference където enabled = true
     */
    List<NotificationPreference> findAllByEnabledIsTrue();
}
```

---

## 🔧 Промени в DojoCardIndex

### 1️⃣ Нов DTO - BroadcastNotificationRequest

**Файл:** `notification/client/dto/BroadcastNotificationRequest.java`

```java
package cardindex.dojocardindex.notification.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BroadcastNotificationRequest {
    
    /**
     * Заглавие на нотификацията
     */
    private String title;
    
    /**
     * Съдържание на нотификацията
     */
    private String content;
    
    /**
     * Потребители за изключване (напр. автора)
     */
    @Builder.Default
    private List<UUID> excludeUserIds = List.of();
}
```

---

### 2️⃣ Обновяване на NotificationClient

**Файл:** `notification/client/NotificationClient.java`

**Добави нов метод:**

```java
package cardindex.dojocardindex.notification.client;

import cardindex.dojocardindex.notification.client.dto.BroadcastNotificationRequest;
import cardindex.dojocardindex.notification.client.dto.Notification;
import cardindex.dojocardindex.notification.client.dto.NotificationPreferenceRequest;
import cardindex.dojocardindex.notification.client.dto.NotificationPreference;
import cardindex.dojocardindex.notification.client.dto.NotificationRequest;
import cardindex.dojocardindex.web.dto.ForgottenPasswordRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "mail-svc", url = "https://mail-svc-app-container-app.yellowdesert-a725fdfe.switzerlandnorth.azurecontainerapps.io/api/v1/notifications")
public interface NotificationClient {

    // ...existing methods...

    /**
     * Изпраща broadcast нотификация до всички потребители
     * 
     * @param request BroadcastNotificationRequest
     * @return ResponseEntity<Void> - 202 Accepted
     */
    @PostMapping("/broadcast")
    ResponseEntity<Void> broadcastNotification(@RequestBody BroadcastNotificationRequest request);
}
```

---

### 3️⃣ Обновяване на NotificationService

**Файл:** `notification/service/NotificationService.java`

**Добави нов метод:**

```java
package cardindex.dojocardindex.notification.service;

import cardindex.dojocardindex.notification.client.NotificationClient;
import cardindex.dojocardindex.notification.client.dto.BroadcastNotificationRequest;
// ...existing imports...

@Slf4j
@Service
public class NotificationService {

    private final NotificationClient notificationClient;

    @Autowired
    public NotificationService(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    // ...existing methods...

    /**
     * Изпраща broadcast нотификация до всички потребители
     * Използва се при създаване на нов Post
     * 
     * @param postTitle Заглавие на поста
     * @param postContent Съдържание на поста
     * @param excludeUserIds Потребители за изключване (напр. автора)
     */
    @Async
    public void broadcastNewPostNotification(String postTitle, String postContent, List<UUID> excludeUserIds) {
        try {
            BroadcastNotificationRequest request = BroadcastNotificationRequest.builder()
                .title("Нова публикация: " + postTitle)
                .content(postContent)
                .excludeUserIds(excludeUserIds != null ? excludeUserIds : List.of())
                .build();
            
            log.info("Изпращане на broadcast нотификация за нова публикация: [{}]", postTitle);
            
            ResponseEntity<Void> response = notificationClient.broadcastNotification(request);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Broadcast нотификация изпратена успешно за пост: [{}]", postTitle);
            } else {
                log.error("Broadcast нотификация неуспешна. HTTP статус: [{}]", 
                    response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Грешка при изпращане на broadcast нотификация за пост: [{}]", 
                postTitle, e);
        }
    }
    
    /**
     * Overload метод без excludeUserIds
     */
    @Async
    public void broadcastNewPostNotification(String postTitle, String postContent) {
        broadcastNewPostNotification(postTitle, postContent, List.of());
    }
}
```

---

### 4️⃣ Обновяване на PostService

**Файл:** `Post/Service/PostService.java`

**Промени:**

```java
package cardindex.dojocardindex.Post.Service;

import cardindex.dojocardindex.Comment.models.Comment;
import cardindex.dojocardindex.Post.Repository.PostRepository;
import cardindex.dojocardindex.Post.models.Post;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.notification.service.NotificationService; // ✅ ДОБАВИ
import cardindex.dojocardindex.web.dto.CreatePostRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserService userService;
    private final NotificationService notificationService; // ✅ ДОБАВИ

    @Autowired
    public PostService(
            PostRepository postRepository, 
            UserService userService,
            NotificationService notificationService) { // ✅ ДОБАВИ
        this.postRepository = postRepository;
        this.userService = userService;
        this.notificationService = notificationService; // ✅ ДОБАВИ
    }

    public List<Post> getAllUnreadPosts(){
        return postRepository.findAllByIsReadIsFalse(Sort.by(Sort.Order.desc("created")));
    }

    public void createNewPost(CreatePostRequest createPostRequest){
        User sender = userService.getCurrentUser();

        Post post = Post.builder()
                .author(sender)
                .created(LocalDateTime.now())
                .title(createPostRequest.getTitle())
                .content(createPostRequest.getContent())
                .isRead(false)
                .build();

        postRepository.save(post);
        
        // ✅ ДОБАВИ: Изпрати broadcast нотификация
        // Изключваме автора от нотификациите
        notificationService.broadcastNewPostNotification(
            post.getTitle(),
            post.getContent(),
            List.of(sender.getId()) // excludeUserIds - автора не получава нотификация
        );
        
        log.info("Нов пост създаден и broadcast нотификация изпратена: [{}]", post.getTitle());
    }

    // ...existing methods...
}
```

---

## 🔄 Поток на данните

### Стъпка по стъпка:

```
1. Потребител създава нов Post чрез UI
   └─> POST /posts/create

2. PostController получава заявката
   └─> postService.createNewPost(createPostRequest)

3. PostService.createNewPost():
   ├─> Създава Post обект
   ├─> Запазва в базата данни (postRepository.save())
   └─> Извиква notificationService.broadcastNewPostNotification()

4. NotificationService.broadcastNewPostNotification() (@Async):
   ├─> Създава BroadcastNotificationRequest
   │   ├─ title: "Нова публикация: {postTitle}"
   │   ├─ content: {postContent}
   │   └─ excludeUserIds: [authorId]
   └─> Извиква notificationClient.broadcastNotification(request)

5. Feign Client:
   └─> HTTP POST към mail-svc
       URL: https://mail-svc.../api/v1/notifications/broadcast
       Body: BroadcastNotificationRequest JSON

6. mail-svc NotificationController.broadcastNotification():
   ├─> Валидира заявката
   ├─> Извиква notificationService.broadcastNotificationAsync()
   └─> Връща 202 Accepted (веднага)

7. mail-svc NotificationService.broadcastNotificationAsync() (@Async):
   ├─> Намира всички NotificationPreference с enabled = true
   ├─> Филтрира excludeUserIds
   ├─> Разделя на батчове от 10 потребители
   └─> За всеки батч:
       ├─> Изпраща мейл на всеки потребител
       ├─> Записва в notification history
       └─> Изчаква 1 секунда преди следващия батч

8. КРАЙ
   └─> Всички потребители с enabled notifications получават мейл
```

---

## ✅ Предимства

### Производителност

| Метрика | Преди (N заявки) | След (Batch) | Подобрение |
|---------|------------------|--------------|------------|
| HTTP заявки от DojoCardIndex | N | 1 | **N× по-малко** |
| Време за отговор | N × 200ms | ~50ms | **~4000× по-бързо** (за 100 users) |
| Network overhead | N × payload size | 1 × small payload | **Драстично намален** |
| Memory usage в DojoCardIndex | Висок (N паралелни заявки) | Нисък (1 заявка) | **N× по-малко** |

### Scalability

- ✅ **DojoCardIndex е отговорен само за 1 заявка** - независимо от броя потребители
- ✅ **mail-svc контролира темпото** на изпращане (throttling)
- ✅ **По-лесно мащабиране** - може да се добави queue (RabbitMQ) само в mail-svc
- ✅ **Rate limiting** се справя на едно място

### Reliability

- ✅ **Retry логика** може да се добави само в mail-svc
- ✅ **Error handling** е централизиран
- ✅ **Monitoring** е по-лесен (1 endpoint вместо N заявки)
- ✅ **Logging** е по-чист и организиран

### Maintainability

- ✅ **Separation of concerns** - DojoCardIndex не знае как се изпращат broadcast мейлите
- ✅ **По-лесно тестване** - можеш да mock-неш 1 заявка
- ✅ **Конфигурация** на throttling е само в mail-svc

---

## 🆚 Сравнение на решенията

### Вариант 1: Текущ подход (N individual calls)

```java
// PostService.createNewPost()
List<User> users = userRepository.findAllByStatus(UserStatus.ACTIVE);
for (User user : users) {
    notificationService.sendNotification(
        user.getId(),
        user.getFirstName(),
        user.getLastName(),
        "Нов пост: " + post.getTitle(),
        post.getContent()
    );
}
```

**Плюсове:**
- ✅ Лесна имплементация (вече има метода)
- ✅ Не изисква промени в mail-svc

**Минуси:**
- ❌ N HTTP заявки
- ❌ Бавно при много потребители
- ❌ Високо натоварване
- ❌ Риск от timeout
- ❌ Труден rate limiting

**Подходящо за:** < 20 потребители

---

### Вариант 2: Batch API (това решение)

```java
// PostService.createNewPost()
notificationService.broadcastNewPostNotification(
    post.getTitle(),
    post.getContent(),
    List.of(sender.getId())
);
```

**Плюсове:**
- ✅ 1 HTTP заявка
- ✅ Бързо независимо от броя потребители
- ✅ Контролирано натоварване
- ✅ Лесен rate limiting
- ✅ Scalable решение

**Минуси:**
- ❌ Изисква промени в mail-svc
- ❌ По-сложна имплементация

**Подходящо за:** Всякакъв брой потребители

---

### Вариант 3: Message Queue (RabbitMQ/Kafka)

```java
// PostService.createNewPost()
messageQueue.publish("new-post-event", {
    postId: post.getId(),
    title: post.getTitle(),
    content: post.getContent()
});
```

**Плюсове:**
- ✅ Пълно декуплиране
- ✅ Retry mechanism
- ✅ Event-driven architecture
- ✅ Максимална scalability

**Минуси:**
- ❌ Изисква допълнителна инфраструктура (RabbitMQ/Kafka)
- ❌ По-сложна архитектура
- ❌ Повече moving parts
- ❌ Допълнителна цена (Azure Service Bus)

**Подходящо за:** Голям брой потребители (1000+) или event-driven системи

---

## 🔧 Конфигурация и тунинг

### Настройки в mail-svc

```java
// application.properties

# Async thread pool за broadcast
spring.task.execution.pool.core-size=5
spring.task.execution.pool.max-size=10
spring.task.execution.pool.queue-capacity=100

# Broadcast параметри
notification.broadcast.batch-size=10
notification.broadcast.delay-between-batches-ms=1000
notification.broadcast.max-retries=3
```

### Използване на конфигурацията

```java
@Service
public class NotificationService {
    
    @Value("${notification.broadcast.batch-size:10}")
    private int batchSize;
    
    @Value("${notification.broadcast.delay-between-batches-ms:1000}")
    private int delayBetweenBatches;
    
    private void broadcastInBatches(...) {
        for (int i = 0; i < targetUsers.size(); i += batchSize) {
            // ...
            Thread.sleep(delayBetweenBatches);
        }
    }
}
```

---

## 📊 Примерни сценарии

### Сценарий 1: 10 потребители

**Преди:**
- 10 HTTP заявки × 200ms = **2 секунди**
- 10 паралелни SMTP заявки

**След:**
- 1 HTTP заявка = **50ms**
- mail-svc изпраща 10 мейла за **~2 секунди** (throttled)

**Резултат:** DojoCardIndex отговаря 40× по-бързо

---

### Сценарий 2: 100 потребители

**Преди:**
- 100 HTTP заявки × 200ms = **20 секунди**
- Възможен timeout
- Rate limiting риск

**След:**
- 1 HTTP заявка = **50ms**
- mail-svc изпраща 100 мейла за **~10 секунди** (10 батча × 1s)

**Резултат:** DojoCardIndex отговаря 400× по-бързо + няма риск от timeout

---

### Сценарий 3: 500 потребители

**Преди:**
- 500 HTTP заявки = **НЕВЪЗМОЖНО**
- Гарантиран timeout
- Вероятно бан от SMTP provider

**След:**
- 1 HTTP заявка = **50ms**
- mail-svc изпраща 500 мейла за **~50 секунди** (50 батча × 1s)

**Резултат:** РАБОТИ без проблеми

---

## 🧪 Тестване

### Unit Test за NotificationService (DojoCardIndex)

```java
@SpringBootTest
class NotificationServiceTest {
    
    @MockBean
    private NotificationClient notificationClient;
    
    @Autowired
    private NotificationService notificationService;
    
    @Test
    void broadcastNewPostNotification_shouldCallClient() {
        // Arrange
        String title = "Test Post";
        String content = "Test Content";
        UUID authorId = UUID.randomUUID();
        
        when(notificationClient.broadcastNotification(any()))
            .thenReturn(ResponseEntity.accepted().build());
        
        // Act
        notificationService.broadcastNewPostNotification(title, content, List.of(authorId));
        
        // Wait for async
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            // Assert
            ArgumentCaptor<BroadcastNotificationRequest> captor = 
                ArgumentCaptor.forClass(BroadcastNotificationRequest.class);
            
            verify(notificationClient).broadcastNotification(captor.capture());
            
            BroadcastNotificationRequest request = captor.getValue();
            assertEquals("Нова публикация: " + title, request.getTitle());
            assertEquals(content, request.getContent());
            assertTrue(request.getExcludeUserIds().contains(authorId));
        });
    }
}
```

### Integration Test за PostService

```java
@SpringBootTest
@AutoConfigureMockMvc
class PostServiceIntegrationTest {
    
    @Autowired
    private PostService postService;
    
    @MockBean
    private NotificationService notificationService;
    
    @Test
    @WithMockUser(username = "test@example.com")
    void createNewPost_shouldTriggerBroadcastNotification() {
        // Arrange
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle("Integration Test Post");
        request.setContent("Test content");
        
        // Act
        postService.createNewPost(request);
        
        // Assert
        verify(notificationService, timeout(5000))
            .broadcastNewPostNotification(
                eq("Integration Test Post"),
                eq("Test content"),
                anyList()
            );
    }
}
```

---

## 📝 Checklist за имплементация

### mail-svc

- [ ] Създай `BroadcastNotificationRequest` DTO
- [ ] Добави `POST /broadcast` endpoint в `NotificationController`
- [ ] Имплементирай `broadcastNotificationAsync()` в `NotificationService`
- [ ] Имплементирай `broadcastInBatches()` helper метод
- [ ] Имплементирай `sendBroadcastEmail()` helper метод
- [ ] Добави `findAllByEnabledIsTrue()` в `NotificationPreferenceRepository`
- [ ] Тествай с малък брой потребители
- [ ] Тествай с голям брой потребители (load test)
- [ ] Добави конфигурация за batch size и delay
- [ ] Добави monitoring/metrics

### DojoCardIndex

- [ ] Създай `BroadcastNotificationRequest` DTO
- [ ] Добави метод в `NotificationClient` интерфейс
- [ ] Имплементирай `broadcastNewPostNotification()` в `NotificationService`
- [ ] Инжектирай `NotificationService` в `PostService`
- [ ] Добави извикване в `createNewPost()` метод
- [ ] Добави unit тестове
- [ ] Добави integration тестове
- [ ] Тествай end-to-end

---

## ⚠️ Важни забележки

### 1. Асинхронност

И двата метода са `@Async`:
- `NotificationService.broadcastNewPostNotification()` в DojoCardIndex
- `NotificationService.broadcastNotificationAsync()` в mail-svc

Това означава:
- ✅ DojoCardIndex не чака изпращането на мейлите
- ✅ UI отговаря веднага след създаване на поста
- ⚠️ Грешки при изпращане няма да блокират създаването на поста

### 2. Error Handling

При грешка в mail-svc:
- Поста **ЩЕ СЕ СЪЗДАДЕ** успешно
- Мейлите **МОЖЕ ДА НЕ СЕ ИЗПРАТЯТ**
- Грешката ще се логне, но няма да се показва на потребителя

Решение:
- Добави retry логика
- Добави dead letter queue
- Добави admin dashboard за failed notifications

### 3. Exclude User IDs

Автора на поста **НЕ трябва** да получава нотификация за собствения си пост:

```java
notificationService.broadcastNewPostNotification(
    post.getTitle(),
    post.getContent(),
    List.of(sender.getId()) // ✅ Exclude author
);
```

### 4. Throttling

При много потребители, SMTP провайдърите имат limits:
- Gmail: ~500 мейла/ден
- SendGrid Free: 100 мейла/ден
- SendGrid Paid: зависи от плана

**Препоръка:** Използвай професионален SMTP service (SendGrid, AWS SES, Mailgun)

### 5. Cost

Всяко изпращане на broadcast notification ще изпрати N мейла:
- 100 потребители = 100 мейла
- Ако има 10 поста на ден = 1000 мейла/ден

**Препоръка:** Добави опция за digest (напр. 1 мейл на ден с всички нови постове)

---

## 🚀 Бъдещи подобрения

### 1. Digest Notifications

Вместо мейл за всеки пост, изпращай 1 мейл на ден с резюме:

```
Заглавие: "Нови публикации за 24.02.2026"

Съдържание:
- Post 1: "Java Tips" (създаден от Ivan Ivanov)
- Post 2: "Spring Boot Tutorial" (създаден от Maria Petrova)
- Post 3: "Docker Guide" (създаден от Georgi Georgiev)

Вижте всички публикации: [link]
```

### 2. User Preferences

Позволи на потребителите да избират:
- ☑️ Веднага (instant notification)
- ☑️ Дневен digest (1 мейл на ден)
- ☑️ Седмичен digest (1 мейл на седмица)
- ☐ Без нотификации

### 3. Rich Email Templates

Използвай HTML templates вместо plain text:
- Добави logo
- Добави бутони (CTA)
- Добави styling
- Добави unsubscribe link

### 4. Push Notifications

Добави browser push notifications като алтернатива на мейлите:
- По-бързи
- По-евтини
- По-добър user engagement

### 5. Message Queue

При голям мащаб, замени direct HTTP call с message queue:

```
PostService → RabbitMQ → mail-svc
```

### 6. Analytics

Добави tracking:
- Колко мейла са изпратени
- Колко са open rate
- Колко са click rate
- Кои потребители не отварят мейлите

---

## 📚 Допълнителни ресурси

### Документация
- [Spring @Async](https://spring.io/guides/gs/async-method/)
- [OpenFeign Documentation](https://spring.io/projects/spring-cloud-openfeign)
- [Azure Container Apps](https://learn.microsoft.com/en-us/azure/container-apps/)

### Best Practices
- [Email Sending Best Practices](https://sendgrid.com/blog/email-best-practices/)
- [SMTP Rate Limiting](https://www.mailgun.com/blog/deliverability/smtp-rate-limiting/)
- [Microservices Communication Patterns](https://microservices.io/patterns/communication-style/messaging.html)

---

## 📞 Контакти и поддръжка

При проблеми или въпроси:
1. Провери логовете в mail-svc
2. Провери логовете в DojoCardIndex
3. Тествай endpoint-а директно (Postman/curl)
4. Провери Azure Container Apps статус

---

**Дата на създаване:** 24.02.2026  
**Версия:** 1.0  
**Статус:** Draft - готов за review и имплементация

---


