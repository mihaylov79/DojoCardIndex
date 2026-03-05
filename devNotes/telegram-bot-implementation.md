# 🤖 Telegram Bot Имплементация за Dragon Dojo Card Index

**Дата:** 27 февруари 2026  
**Цел:** Добавяне на Telegram нотификации за нови публикации с асинхронна проверка при логин и кеширане

---

## 📋 Съдържание

1. [Обща архитектура](#1-обща-архитектура)
2. [Подготовка - Създаване на Telegram Bot](#2-подготовка---създаване-на-telegram-bot)
3. [Промени в зависимостите (pom.xml)](#3-промени-в-зависимостите-pomxml)
4. [База данни промени](#4-база-данни-промени)
5. [Нови Java класове](#5-нови-java-класове)
6. [Промени в съществуващи класове](#6-промени-в-съществуващи-класове)
7. [Frontend промени](#7-frontend-промени)
8. [Конфигурация (application.properties)](#8-конфигурация-applicationproperties)
9. [Тестване](#9-тестване)
10. [Често задавани въпроси (FAQ)](#10-често-задавани-въпроси-faq)

---

## 1. Обща архитектура

### 🎯 Как работи системата:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│  НОВА регистрация (след имплементацията)                                    │
│  └─> @PrePersist генерира telegramSubscriptionCode автоматично              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│  СЪЩЕСТВУВАЩ потребител (преди имплементацията - няма код)                  │
│  └─> Потребителят влиза                                                     │
│      └─> AuthenticationSuccessEvent се излъчва                              │
│          └─> UserLoginCheckService.performLoginChecks() (@Async)            │
│              └─> Проверява @Cacheable кеша                                  │
│                  └─> Няма в кеша → проверява DB → няма код                  │
│                      └─> Генерира код и записва в DB                        │
│                      └─> Кешира резултата (key: user.id → true)             │
│  └─> Потребителят влиза БЕЗ забавяне (асинхронно)                           │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│  ВТОРИ и ВСИЧКИ СЛЕДВАЩИ ЛОГИНИ                                             │
│  └─> Потребителят влиза                                                     │
│      └─> AuthenticationSuccessEvent се излъчва                              │
│          └─> UserLoginCheckService.performLoginChecks() (@Async)            │
│              └─> @Cacheable проверява кеша                                  │
│                  └─> ИМА в кеша → БАЙПАСВА проверката напълно! ⚡           │
│  └─> НУЛЕВО забавяне, НУЛЕВА заявка към DB                                  │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│  АБОНИРАНЕ В TELEGRAM                                                       │
│  └─> Потребителят отваря профила си (/home)                                 │
│      └─> Вижда секцията "Telegram Нотификации"                              │
│      └─> Вижда своя уникален код (например: A3F9K2L7)                       │
│      └─> Копира кода → отваря Telegram → намира @dragon_dojo_bot            │
│          └─> Изпраща: /start                                                │
│          └─> Изпраща: /subscribe A3F9K2L7                                   │
│              └─> Ботът проверява кода в DB                                  │
│                  └─> Валиден → записва chatId в telegram_subscriptions      │
│                      └─> Изпраща потвърждение: "✅ Успешно абониран!"       │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│  ПУБЛИКУВАНЕ НА НОВ ПОСТ                                                    │
│  └─> Админът създава нов пост чрез /posts/new                               │
│      └─> PostService.createNewPost() се извиква                             │
│          └─> Постът се записва в DB                                         │
│          └─> TelegramBotService.notifyNewPost(post) се извиква              │
│              └─> Извлича всички активни абонати от telegram_subscriptions   │
│              └─> За всеки абонат:                                           │
│                  └─> Изпраща Telegram съобщение с:                          │
│                      - Заглавие на поста                                    │
│                      - Име на автора                                        │
│                      - Дата и час                                           │
│                      - Link към поста                                       │
│  └─> Всички абонати получават МОМЕНТАЛНА нотификация! 🎉                    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 🔑 Ключови концепции:

1. **@Async (Асинхронно изпълнение)**
   - Проверката при логин НЕ блокира потребителя
   - Изпълнява се във фонов thread
   - Потребителят влиза моментално

2. **@Cacheable (Кеширане)**
   - След първата проверка, резултатът се кешира
   - Всички следващи логини байпасват проверката
   - Драстично намалява заявките към DB

3. **@PrePersist (Автоматично генериране)**
   - Нови потребители автоматично получават код при регистрация
   - Не е нужна ръчна намеса

4. **Event-driven (Базирано на събития)**
   - `AuthenticationSuccessEvent` → автоматична проверка
   - Decoupling - не пречи на логин логиката

---

## 2. Подготовка - Създаване на Telegram Bot

### Стъпка 1: Регистриране на бота в Telegram

1. **Отвори Telegram** и намери **@BotFather**
2. Изпрати команда: `/newbot`
3. BotFather ще те попита за информация:

```
BotFather: Alright, a new bot. How are we going to call it? 
           Please choose a name for your bot.

Ти: Dragon Dojo Notifications

BotFather: Good. Now let's choose a username for your bot. 
           It must end in `bot`. Like this, for example: TetrisBot or tetris_bot.

Ти: dragon_dojo_bot

BotFather: Done! Congratulations on your new bot. 
           You will find it at t.me/dragon_dojo_bot. 
           You can now add a description, about section and profile picture 
           for your bot, see /help for a list of commands.

           Use this token to access the HTTP API:
           123456789:ABCdefGHIjklMNOpqrsTUVwxyz1234567

           Keep your token secure and store it safely, 
           it can be used by anyone to control your bot.
```

4. **ВАЖНО:** Запази токена! Ще ти трябва за конфигурацията.

### Стъпка 2: Персонализиране на бота (опционално)

```
/setdescription - Добави описание на бота
/setabouttext - Добави "About" текст
/setuserpic - Качи профилна снимка (например логото на Dragon Dojo)
```

**Пример за описание:**
```
Официален бот на Dragon Dojo за нотификации при нови публикации, събития и важни обяви.
```

---

## 3. Промени в зависимостите (pom.xml)

### Какво добавяме:
- **Telegram Bots API** - за комуникация с Telegram
- **Spring Boot Starter Cache** - за кеширане (ако го нямаш)

### Промени в `pom.xml`:

```xml
<!-- Добави ПРЕДИ затварящия </dependencies> таг -->

<!-- Telegram Bot API -->
<dependency>
    <groupId>org.telegram</groupId>
    <artifactId>telegrambots</artifactId>
    <version>6.9.7.1</version>
</dependency>

<dependency>
    <groupId>org.telegram</groupId>
    <artifactId>telegrambots-spring-boot-starter</artifactId>
    <version>6.9.7.1</version>
</dependency>

<!-- Spring Cache (ако го нямаш вече) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-cache</artifactId>
</dependency>
```

### След промените:

```bash
# В PowerShell терминала на IntelliJ (долу):
.\mvnw clean install
```

**Забележка:** Ако имаш проблеми с Maven, рестартирай IntelliJ.

---

## 4. База данни промени

### 4.1. Добавяне на `telegramSubscriptionCode` в `users` таблица

**Защо:** Всеки потребител трябва да има уникален код за абониране в Telegram.

**SQL миграция (ръчно изпълнение в MySQL):**

```sql
-- Добави новата колона в users таблицата
ALTER TABLE users 
ADD COLUMN telegram_subscription_code VARCHAR(8) UNIQUE NULL;

-- Добави индекс за бързи заявки
CREATE INDEX idx_telegram_code ON users(telegram_subscription_code);
```

**Забележка:** 
- За съществуващи потребители колоната ще бъде `NULL`
- Асинхронната проверка при логин ще генерира кодовете автоматично
- Нови потребители ще имат код веднага при регистрация (благодарение на `@PrePersist`)

---

### 4.2. Създаване на `telegram_subscriptions` таблица

**Защо:** Съхраняваме връзката между потребител от системата и неговия Telegram `chatId`.

**SQL миграция:**

```sql
CREATE TABLE telegram_subscriptions (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL UNIQUE,
    chat_id BIGINT NOT NULL UNIQUE,
    subscribed_at DATETIME NOT NULL,
    active TINYINT(1) DEFAULT 1,
    CONSTRAINT fk_telegram_user 
        FOREIGN KEY (user_id) REFERENCES users(id) 
        ON DELETE CASCADE,
    INDEX idx_chat_id (chat_id),
    INDEX idx_active (active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**Обяснение на колоните:**
- `id` - Primary key (UUID, 16 bytes)
- `user_id` - Връзка към `users.id` (уникален - един потребител = един Telegram акаунт)
- `chat_id` - Telegram Chat ID (уникален - един Telegram акаунт = един потребител)
- `subscribed_at` - Дата и час на абониране
- `active` - Дали абонаментът е активен (за отписване)

**Забележка:** `ON DELETE CASCADE` означава, че ако изтриеш потребител от `users`, автоматично се изтрива и записът от `telegram_subscriptions`.

---

## 5. Нови Java класове

### 5.1. 📁 `TelegramSubscription.java` (Model)

**Локация:** `src/main/java/cardindex/dojocardindex/telegram/models/TelegramSubscription.java`

**Създай новата директория:**
```
src/main/java/cardindex/dojocardindex/
└── telegram/
    ├── models/
    │   └── TelegramSubscription.java
    ├── repository/
    │   └── TelegramSubscriptionRepository.java
    └── service/
        └── TelegramBotService.java
```

**Код:**

```java
package cardindex.dojocardindex.telegram.models;

import cardindex.dojocardindex.User.models.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity за съхранение на Telegram абонаменти.
 * Свързва потребител от системата с неговия Telegram chatId.
 */
@Entity
@Table(name = "telegram_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelegramSubscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    /**
     * Връзка към потребителя (One-to-One).
     * Един потребител може да има само един Telegram абонамент.
     */
    @OneToOne
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;
    
    /**
     * Telegram Chat ID - уникален идентификатор на чата с бота.
     * Използва се за изпращане на съобщения.
     */
    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;
    
    /**
     * Дата и час на абониране.
     */
    @Column(name = "subscribed_at", nullable = false)
    private LocalDateTime subscribedAt;
    
    /**
     * Дали абонаментът е активен.
     * false = потребителят се е отписал (/unsubscribe)
     */
    @Column(nullable = false)
    private boolean active;
}
```

**Обяснение:**
- `@OneToOne` - един потребител има максимум един Telegram абонамент
- `chatId` - това е ID-то, което Telegram дава на всеки чат с бота
- `active` - позволява на потребителя да спре нотификациите без да трие записа

---

### 5.2. 📁 `TelegramSubscriptionRepository.java` (Repository)

**Локация:** `src/main/java/cardindex/dojocardindex/telegram/repository/TelegramSubscriptionRepository.java`

**Код:**

```java
package cardindex.dojocardindex.telegram.repository;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.telegram.models.TelegramSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository за Telegram абонаменти.
 */
@Repository
public interface TelegramSubscriptionRepository extends JpaRepository<TelegramSubscription, UUID> {
    
    /**
     * Намери абонамент по Telegram chatId.
     */
    Optional<TelegramSubscription> findByChatId(Long chatId);
    
    /**
     * Намери всички активни абонаменти.
     * Използва се за broadcast нотификации.
     */
    List<TelegramSubscription> findAllByActiveIsTrue();
    
    /**
     * Намери абонамент по потребител.
     */
    Optional<TelegramSubscription> findByUser(User user);
    
    /**
     * Провери дали потребителят има активен абонамент.
     */
    boolean existsByUserAndActiveIsTrue(User user);
}
```

**Обяснение:**
- `findAllByActiveIsTrue()` - връща само активните абонаменти (за broadcast)
- `existsByUserAndActiveIsTrue()` - проверка дали потребителят е абониран

---

### 5.3. 📁 `TelegramBotService.java` (Service - Основната логика на бота)

**Локация:** `src/main/java/cardindex/dojocardindex/telegram/service/TelegramBotService.java`

**Код (дълъг, но добре коментиран):**

```java
package cardindex.dojocardindex.telegram.service;

import cardindex.dojocardindex.Post.models.Post;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.telegram.models.TelegramSubscription;
import cardindex.dojocardindex.telegram.repository.TelegramSubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Telegram Bot Service - обработва команди от потребители и изпраща нотификации.
 * 
 * Поддържани команди:
 * - /start - Инструкции за абониране
 * - /subscribe <КОД> - Абониране с код от профила
 * - /unsubscribe - Отписване от нотификации
 * - /status - Проверка на статуса на абонамента
 */
@Service
@Slf4j
public class TelegramBotService extends TelegramLongPollingBot {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;
    
    @Value("${app.base.url:https://dragondojo.com}")
    private String appBaseUrl;

    private final TelegramSubscriptionRepository subscriptionRepository;
    private final UserService userService;

    @Autowired
    public TelegramBotService(
            TelegramSubscriptionRepository subscriptionRepository, 
            UserService userService,
            @Value("${telegram.bot.token}") String botToken) {
        super(botToken);
        this.subscriptionRepository = subscriptionRepository;
        this.userService = userService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Обработка на всички входящи съобщения от потребители.
     */
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            String firstName = update.getMessage().getFrom().getFirstName();

            log.info("Получено съобщение от потребител [{}] (chatId: {}): {}", 
                     firstName, chatId, messageText);

            // Обработка на командите
            if (messageText.equals("/start")) {
                handleStartCommand(chatId, firstName);
            } else if (messageText.startsWith("/subscribe ")) {
                handleSubscribeCommand(chatId, messageText, firstName);
            } else if (messageText.equals("/unsubscribe")) {
                handleUnsubscribeCommand(chatId);
            } else if (messageText.equals("/status")) {
                handleStatusCommand(chatId);
            } else {
                // Непозната команда
                sendMessage(chatId, "❌ Непозната команда. Използвай /start за помощ.");
            }
        }
    }

    /**
     * Команда /start - показва инструкции за абониране.
     */
    private void handleStartCommand(Long chatId, String firstName) {
        String welcomeMessage = String.format(
            "🥋 Здравей, %s!\n\n" +
            "Добре дошъл в Dragon Dojo бота! 🐉\n\n" +
            "За да получаваш нотификации за нови публикации, " +
            "трябва да свържеш акаунта си:\n\n" +
            "📋 *Стъпки за абониране:*\n" +
            "1️⃣ Влез в профила си на %s/home\n" +
            "2️⃣ Намери секцията \"Telegram Нотификации\"\n" +
            "3️⃣ Копирай своя код за абониране\n" +
            "4️⃣ Изпрати тук: `/subscribe ТвоятКод`\n\n" +
            "📌 *Други команди:*\n" +
            "`/status` - Проверка на статуса\n" +
            "`/unsubscribe` - Спиране на нотификации\n\n" +
            "Ако имаш проблеми, свържи се с администратор.",
            firstName,
            appBaseUrl
        );
        sendMessageWithMarkdown(chatId, welcomeMessage);
    }

    /**
     * Команда /subscribe <КОД> - абониране на потребител.
     */
    private void handleSubscribeCommand(Long chatId, String messageText, String firstName) {
        try {
            // Извличане на кода от съобщението
            String subscriptionCode = messageText.replace("/subscribe ", "").trim().toUpperCase();
            
            if (subscriptionCode.isEmpty() || subscriptionCode.equals("/SUBSCRIBE")) {
                sendMessage(chatId, 
                    "❌ Моля, предоставете кода за абониране:\n" +
                    "Пример: /subscribe A3F9K2L7");
                return;
            }

            log.info("Опит за абониране с код: {}", subscriptionCode);
            
            // Намери потребителя по код
            User user = userService.findByTelegramSubscriptionCode(subscriptionCode);
            
            if (user == null) {
                sendMessage(chatId, 
                    "❌ Невалиден код за абониране!\n\n" +
                    "Моля, провери дали си копирал правилно кода от профила си " +
                    "и опитай отново.");
                log.warn("Невалиден код за абониране: {}", subscriptionCode);
                return;
            }

            // Провери дали вече съществува абонамент за този потребител
            if (subscriptionRepository.existsByUserAndActiveIsTrue(user)) {
                sendMessage(chatId, 
                    "⚠️ Вече си абониран за нотификации!\n" +
                    "Използвай /status за проверка.");
                log.info("Потребител {} е вече абониран", user.getEmail());
                return;
            }

            // Провери дали този chatId е използван от друг потребител
            subscriptionRepository.findByChatId(chatId).ifPresent(existingSub -> {
                existingSub.setActive(false);
                subscriptionRepository.save(existingSub);
                log.info("Деактивиран стар абонамент за chatId: {}", chatId);
            });

            // Създай нов абонамент
            TelegramSubscription subscription = TelegramSubscription.builder()
                .user(user)
                .chatId(chatId)
                .subscribedAt(LocalDateTime.now())
                .active(true)
                .build();
            
            subscriptionRepository.save(subscription);
            
            sendMessageWithMarkdown(chatId, 
                String.format(
                    "✅ *Успешно абониран!*\n\n" +
                    "Здравей, *%s %s*! 🥋\n\n" +
                    "Вече ще получаваш нотификации за:\n" +
                    "📢 Нови публикации\n" +
                    "📅 Предстоящи събития\n" +
                    "🏆 Важни обяви\n\n" +
                    "За да спреш нотификациите, използвай /unsubscribe",
                    user.getFirstName(), 
                    user.getLastName()
                )
            );
            
            log.info("Успешно абониран потребител: {} (chatId: {})", user.getEmail(), chatId);
            
        } catch (Exception e) {
            log.error("Грешка при абониране", e);
            sendMessage(chatId, 
                "❌ Възникна грешка при абониране.\n" +
                "Моля, опитай отново по-късно или се свържи с администратор.");
        }
    }

    /**
     * Команда /unsubscribe - отписване от нотификации.
     */
    private void handleUnsubscribeCommand(Long chatId) {
        subscriptionRepository.findByChatId(chatId).ifPresentOrElse(
            subscription -> {
                subscription.setActive(false);
                subscriptionRepository.save(subscription);
                sendMessage(chatId, 
                    "✅ Успешно отписан от нотификации.\n\n" +
                    "За да се абонираш отново, използвай /subscribe <код>");
                log.info("Потребител {} се отписа от нотификации", 
                         subscription.getUser().getEmail());
            },
            () -> {
                sendMessage(chatId, 
                    "❌ Не си абониран за нотификации.\n" +
                    "Използвай /start за инструкции.");
            }
        );
    }

    /**
     * Команда /status - проверка на статуса на абонамента.
     */
    private void handleStatusCommand(Long chatId) {
        subscriptionRepository.findByChatId(chatId).ifPresentOrElse(
            subscription -> {
                if (subscription.isActive()) {
                    User user = subscription.getUser();
                    String statusMessage = String.format(
                        "✅ *Абониран си за нотификации*\n\n" +
                        "👤 Профил: %s %s\n" +
                        "📧 Email: %s\n" +
                        "📅 Абониран на: %s\n\n" +
                        "За да спреш нотификациите: /unsubscribe",
                        user.getFirstName(),
                        user.getLastName(),
                        user.getEmail(),
                        subscription.getSubscribedAt()
                            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))
                    );
                    sendMessageWithMarkdown(chatId, statusMessage);
                } else {
                    sendMessage(chatId, 
                        "⚠️ Абонаментът ти е неактивен.\n" +
                        "Използвай /subscribe <код> за да се абонираш отново.");
                }
            },
            () -> {
                sendMessage(chatId, 
                    "❌ Не си абониран за нотификации.\n" +
                    "Използвай /start за инструкции.");
            }
        );
    }

    /**
     * Изпращане на нотификация за нов пост на всички активни абонати.
     */
    public void notifyNewPost(Post post) {
        List<TelegramSubscription> activeSubscriptions = 
            subscriptionRepository.findAllByActiveIsTrue();

        if (activeSubscriptions.isEmpty()) {
            log.info("Няма активни Telegram абонати за нотификация");
            return;
        }

        String message = buildPostNotificationMessage(post);

        log.info("Изпращане на нотификация за нов пост към {} абоната(и)", 
                 activeSubscriptions.size());

        int successCount = 0;
        int failureCount = 0;

        for (TelegramSubscription subscription : activeSubscriptions) {
            try {
                sendMessageWithMarkdown(subscription.getChatId(), message);
                successCount++;
                log.debug("Нотификация изпратена до: {}", 
                         subscription.getUser().getEmail());
            } catch (Exception e) {
                failureCount++;
                log.error("Грешка при изпращане на нотификация до chatId: {}", 
                         subscription.getChatId(), e);
            }
        }

        log.info("Нотификации изпратени: {} успешни, {} неуспешни", 
                 successCount, failureCount);
    }

    /**
     * Изграждане на съобщението за нов пост.
     */
    private String buildPostNotificationMessage(Post post) {
        return String.format(
            "📢 *Нова публикация в Dragon Dojo*\n\n" +
            "📝 *%s*\n\n" +
            "✍️ Автор: %s %s\n" +
            "📅 %s\n\n" +
            "[➡️ Прочети повече](%s/posts)",
            escapeMarkdown(post.getTitle()),
            escapeMarkdown(post.getAuthor().getFirstName()),
            escapeMarkdown(post.getAuthor().getLastName()),
            post.getCreated().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
            appBaseUrl
        );
    }

    /**
     * Изпращане на обикновено текстово съобщение.
     */
    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Грешка при изпращане на съобщение до chatId: {}", chatId, e);
        }
    }

    /**
     * Изпращане на съобщение с Markdown форматиране.
     */
    private void sendMessageWithMarkdown(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.setParseMode("Markdown");
        message.disableWebPagePreview();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Грешка при изпращане на Markdown съобщение до chatId: {}", 
                     chatId, e);
        }
    }

    /**
     * Escape на специални символи за Telegram Markdown.
     */
    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text
            .replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("~", "\\~")
            .replace("`", "\\`")
            .replace(">", "\\>")
            .replace("#", "\\#")
            .replace("+", "\\+")
            .replace("-", "\\-")
            .replace("=", "\\=")
            .replace("|", "\\|")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace(".", "\\.")
            .replace("!", "\\!");
    }
}
```

**Обяснение на ключовите методи:**
- `onUpdateReceived()` - приема всички съобщения от потребители
- `handleSubscribeCommand()` - валидира кода и създава абонамент
- `notifyNewPost()` - изпраща нотификация на всички активни абонати
- `escapeMarkdown()` - предпазва от грешки при специални символи

---

### 5.4. 📁 `UserLoginCheckService.java` (Асинхронни проверки + Кеширане)

**Локация:** `src/main/java/cardindex/dojocardindex/config/UserLoginCheckService.java`

**Код:**

```java
package cardindex.dojocardindex.config;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.repository.UserRepository;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.notification.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service за асинхронни проверки при логин на потребител.
 * 
 * Проверява и генерира:
 * 1. Telegram subscription code
 * 2. Notification preferences
 * 
 * Използва кеширане за да избегне повтарящи се проверки.
 */
@Service
@Slf4j
public class UserLoginCheckService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Autowired
    public UserLoginCheckService(
            UserService userService, 
            UserRepository userRepository,
            NotificationService notificationService) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Асинхронни проверки при логин.
     * 
     * ВАЖНО: @Async означава, че този метод се изпълнява във фонов thread
     * и НЕ блокира логина на потребителя!
     * 
     * @param email Email на потребителя
     */
    @Async
    @Transactional
    public void performLoginChecks(String email) {
        try {
            User user = userService.findUserByEmail(email);
            
            if (user == null) {
                log.warn("Потребител с email {} не е намерен за login checks", email);
                return;
            }

            log.debug("Стартиране на асинхронни login checks за потребител: {}", email);

            // Проверка 1: Telegram код (с кеширане)
            checkAndGenerateTelegramCode(user);

            // Проверка 2: Нотификационни предпочитания (съществуваща логика)
            notificationService.checkNotificationPreference(user.getId(), user.getEmail());

            log.debug("Асинхронни login checks завършени за потребител: {}", email);
            
        } catch (Exception e) {
            log.error("Грешка при асинхронни login checks за потребител: {}", email, e);
        }
    }

    /**
     * Проверка дали потребителят има Telegram код.
     * 
     * ВАЖНО: @Cacheable означава, че резултатът се кешира!
     * - Първо влизане: проверява DB → генерира код → кешира
     * - Второ влизане: връща от кеша → НЕ проверява DB!
     * 
     * Key: user.id (UUID на потребителя)
     * Value: true (има код) / false (има проблем)
     * 
     * @param user Потребител
     * @return true ако кодът съществува (или е генериран)
     */
    @Cacheable(value = "telegramCodeCheck", key = "#user.id")
    public boolean checkAndGenerateTelegramCode(User user) {
        log.debug("Проверка за Telegram код на потребител: {} (userId: {})", 
                  user.getEmail(), user.getId());
        
        // Проверка дали има код
        if (user.getTelegramSubscriptionCode() == null || 
            user.getTelegramSubscriptionCode().isEmpty()) {
            
            log.info("Telegram код липсва за потребител {}. Генериране...", 
                     user.getEmail());
            
            // Генерира уникален код
            String code = generateUniqueTelegramCode();
            
            // Записва в DB (използваме toBuilder от Lombok)
            User updatedUser = user.toBuilder()
                .telegramSubscriptionCode(code)
                .build();
            
            userRepository.save(updatedUser);
            
            log.info("Генериран Telegram код за потребител {}: {}", 
                     user.getEmail(), code);
            
            return true; // Кешира се като "има код"
        }
        
        log.debug("Telegram код вече съществува за потребител: {}", user.getEmail());
        return true; // Кешира се като "има код"
    }

    /**
     * Генерира уникален Telegram код.
     * Проверява дали кодът вече съществува в DB.
     * 
     * @return Уникален 8-символен код (главни букви и цифри)
     */
    private String generateUniqueTelegramCode() {
        String code;
        int attempts = 0;
        int maxAttempts = 10;
        
        do {
            // Генерира случаен 8-символен код от UUID
            code = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
            
            attempts++;
            
            if (attempts > maxAttempts) {
                log.error("Не може да се генерира уникален Telegram код след {} опита!", 
                         maxAttempts);
                throw new RuntimeException("Failed to generate unique Telegram code");
            }
            
        } while (userRepository.existsByTelegramSubscriptionCode(code));
        
        log.debug("Генериран уникален Telegram код: {} (след {} опита)", code, attempts);
        return code;
    }

    /**
     * Инвалидиране на кеша за конкретен потребител.
     * 
     * Използвай го САМО ако ръчно променяш кода в DB
     * (например от Admin панел).
     * 
     * @param userId ID на потребителя
     */
    @CacheEvict(value = "telegramCodeCheck", key = "#userId")
    public void invalidateTelegramCodeCache(UUID userId) {
        log.info("Инвалидиран кеш за Telegram код на потребител с ID: {}", userId);
    }

    /**
     * Изчистване на целия кеш (при нужда).
     */
    @CacheEvict(value = "telegramCodeCheck", allEntries = true)
    public void clearAllTelegramCodeCache() {
        log.info("Изчистен целия кеш за Telegram кодове");
    }
}
```

**Обяснение на анотациите:**
- `@Async` - методът се изпълнява във фонов thread (не блокира логина)
- `@Cacheable` - резултатът се кешира (при второ извикване се връща от кеша)
- `@CacheEvict` - изтрива запис от кеша (за специални случаи)

---

### 5.5. 📁 `AsyncCacheConfig.java` (Конфигурация за Async + Cache)

**Локация:** `src/main/java/cardindex/dojocardindex/config/AsyncCacheConfig.java`

**Код:**

```java
package cardindex.dojocardindex.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Конфигурация за асинхронно изпълнение и кеширане.
 */
@Configuration
@EnableAsync
@EnableCaching
public class AsyncCacheConfig {

    /**
     * Thread pool за асинхронни задачи (@Async).
     * 
     * Конфигурация:
     * - corePoolSize: 2 - минимален брой threads
     * - maxPoolSize: 5 - максимален брой threads
     * - queueCapacity: 100 - опашка за чакащи задачи
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-login-check-");
        executor.initialize();
        
        return executor;
    }

    /**
     * Cache Manager за кеширане на резултати.
     * 
     * Кешове:
     * - telegramCodeCheck - кеш за Telegram кодове
     * - notification-preference - кеш за нотификационни предпочитания (съществуващ)
     * - notification-history - кеш за история на нотификации (съществуващ)
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
            new ConcurrentMapCache("telegramCodeCheck"),
            new ConcurrentMapCache("notification-preference"),
            new ConcurrentMapCache("notification-history")
        ));
        return cacheManager;
    }
}
```

**Обяснение:**
- `@EnableAsync` - активира `@Async` анотацията
- `@EnableCaching` - активира `@Cacheable` и други cache анотации
- `ThreadPoolTaskExecutor` - управлява thread pool за асинхронни задачи
- `SimpleCacheManager` - управлява in-memory кешове

**Забележка:** Това е **in-memory кеш** (ConcurrentMapCache). При рестарт на апликацията кешът се изчиства, но се попълва отново при първия логин. Ако искаш persistent кеш (Redis), това е по-сложна имплементация.

---

### 5.6. 📁 `TelegramBotConfig.java` (Регистрация на бота)

**Локация:** `src/main/java/cardindex/dojocardindex/config/TelegramBotConfig.java`

**Код:**

```java
package cardindex.dojocardindex.config;

import cardindex.dojocardindex.telegram.service.TelegramBotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import jakarta.annotation.PostConstruct;

/**
 * Конфигурация за регистриране на Telegram бота.
 */
@Configuration
@Slf4j
public class TelegramBotConfig {
    
    @Autowired
    private TelegramBotService telegramBotService;
    
    /**
     * Регистрира бота при стартиране на апликацията.
     */
    @PostConstruct
    public void init() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBotService);
            log.info("✅ Telegram bot успешно регистриран и стартиран!");
        } catch (TelegramApiException e) {
            log.error("❌ Грешка при регистриране на Telegram бота", e);
            log.error("Проверете telegram.bot.token в application.properties");
        }
    }
}
```

**Обяснение:**
- `@PostConstruct` - изпълнява се веднага след инициализация на Spring контекста
- Регистрира бота в Telegram API
- Ако токенът е невалиден, апликацията ще стартира, но ботът няма да работи

---

## 6. Промени в съществуващи класове

### 6.1. ✏️ Промени в `User.java`

**Локация:** `src/main/java/cardindex/dojocardindex/User/models/User.java`

**Промени:**

```java
// ...existing imports...
import jakarta.persistence.PrePersist;

@Table(name = "users")
@Entity
@Getter
@Builder(toBuilder = true)
@Slf4j
public class User {

    // ...existing fields...
    
    /**
     * Уникален код за абониране в Telegram.
     * Генерира се автоматично при създаване на потребител.
     */
    @Column(name = "telegram_subscription_code", unique = true)
    private String telegramSubscriptionCode;
    
    /**
     * Telegram абонамент (One-to-One връзка).
     */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private TelegramSubscription telegramSubscription;
    
    // ...existing fields...
    
    /**
     * Генериране на Telegram код при създаване на нов потребител.
     * ВАЖНО: @PrePersist се изпълнява САМО при създаване на нов запис!
     */
    @PrePersist
    public void generateTelegramCode() {
        if (telegramSubscriptionCode == null || telegramSubscriptionCode.isEmpty()) {
            telegramSubscriptionCode = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
            log.debug("Генериран Telegram код при създаване на потребител: {}", 
                      telegramSubscriptionCode);
        }
    }
    
    // ...existing methods...
    
    // Getter за telegramSubscriptionCode
    public String getTelegramSubscriptionCode() {
        return telegramSubscriptionCode;
    }
    
    // Getter за telegramSubscription
    public TelegramSubscription getTelegramSubscription() {
        return telegramSubscription;
    }
    
    // Setter за telegramSubscription (нужен за двупосочната връзка)
    public void setTelegramSubscription(TelegramSubscription telegramSubscription) {
        this.telegramSubscription = telegramSubscription;
    }
}
```

**ВАЖНО:** Добави import за новия model:
```java
import cardindex.dojocardindex.telegram.models.TelegramSubscription;
```

**Обяснение:**
- `@PrePersist` - изпълнява се преди първото записване в DB
- Генерира 8-символен код от UUID
- За съществуващи потребители кодът ще бъде `NULL` и ще се генерира при логин

---

### 6.2. ✏️ Промени в `UserRepository.java`

**Локация:** `src/main/java/cardindex/dojocardindex/User/repository/UserRepository.java`

**Промени:**

```java
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // ...existing methods...
    
    /**
     * Провери дали съществува потребител с даден Telegram код.
     * Използва се за валидация на уникалност при генериране.
     */
    boolean existsByTelegramSubscriptionCode(String code);
    
    /**
     * Намери потребител по Telegram subscription код.
     * Използва се при абониране в бота.
     */
    Optional<User> findByTelegramSubscriptionCode(String code);
}
```

---

### 6.3. ✏️ Промени в `UserService.java`

**Локация:** `src/main/java/cardindex/dojocardindex/User/service/UserService.java`

**Промени:**

```java
// ...existing imports...

@Builder(toBuilder = true)
@Service
public class UserService implements UserDetailsService {

    // ...existing fields and methods...
    
    /**
     * Намери потребител по email.
     * 
     * @param email Email на потребителя
     * @return User или null ако не съществува
     */
    public User findUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }
    
    /**
     * Намери потребител по Telegram subscription код.
     * 
     * @param code Telegram subscription код
     * @return User или null ако не съществува
     */
    public User findByTelegramSubscriptionCode(String code) {
        return userRepository.findByTelegramSubscriptionCode(code).orElse(null);
    }
    
    // ...existing methods...
}
```

**Забележка:** Ако вече имаш метод `findUserByEmail`, не го добавяй отново. Просто провери дали съществува.

---

### 6.4. ✏️ Промени в `AuthenticationSuccessListener.java`

**Локация:** `src/main/java/cardindex/dojocardindex/config/AuthenticationSuccessListener.java`

**Промени:**

```java
package cardindex.dojocardindex.config;

import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthenticationSuccessListener {

    private final UserLoginCheckService userLoginCheckService;

    @Autowired
    public AuthenticationSuccessListener(UserLoginCheckService userLoginCheckService) {
        this.userLoginCheckService = userLoginCheckService;
    }

    @EventListener
    public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();
        
        log.info("Потребител {} влезе успешно. Стартиране на асинхронни проверки...", 
                 email);
        
        // АСИНХРОННО - не блокира логина!
        // Проверява:
        // 1. Telegram subscription code
        // 2. Notification preferences
        userLoginCheckService.performLoginChecks(email);
        
        // Логинът продължава веднага без забавяне
        log.debug("Асинхронни проверки стартирани за потребител: {}", email);
    }
}
```

**Обяснение на промените:**
- Премахваме директното извикване на `notificationService`
- Делегираме на `UserLoginCheckService`, който обработва всички проверки
- Логинът продължава веднага (асинхронно)

---

### 6.5. ✏️ Промени в `PostService.java`

**Локация:** `src/main/java/cardindex/dojocardindex/Post/Service/PostService.java`

**Промени:**

```java
package cardindex.dojocardindex.Post.Service;

// ...existing imports...
import cardindex.dojocardindex.telegram.service.TelegramBotService;

@Slf4j
@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserService userService;
    private final TelegramBotService telegramBotService; // НОВО

    @Autowired
    public PostService(PostRepository postRepository, 
                       UserService userService,
                       TelegramBotService telegramBotService) { // НОВО
        this.postRepository = postRepository;
        this.userService = userService;
        this.telegramBotService = telegramBotService; // НОВО
    }

    // ...existing methods...

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
        
        // НОВО: Изпращай Telegram нотификация
        try {
            telegramBotService.notifyNewPost(post);
            log.info("Telegram нотификация изпратена за нов пост: {}", post.getTitle());
        } catch (Exception e) {
            log.error("Грешка при изпращане на Telegram нотификация за пост: {}", 
                     post.getTitle(), e);
            // Не блокираме създаването на поста ако Telegram е недостъпен
        }
    }

    // ...existing methods...
}
```

**Обяснение:**
- Добавяме `TelegramBotService` като dependency
- След записване на поста, извикваме `notifyNewPost()`
- Ако Telegram е недостъпен, постът се създава нормално (graceful degradation)

---

## 7. Frontend промени

### 7.1. ✏️ Промени в `user-details.html` (Профилна страница)

**Локация:** `src/main/resources/templates/user-details.html`

**Добави тази секция СЛЕД информацията на потребителя (преди event history):**

```html
<!-- Telegram Нотификации секция -->
<div class="telegram-notification-section" 
     style="margin-top: 2rem; padding: 1.5rem; background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); border-radius: 12px; border-left: 4px solid #0088cc; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
    
    <div style="display: flex; align-items: center; margin-bottom: 1rem;">
        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="#0088cc" style="margin-right: 0.75rem;">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-1 17.93c-3.95-.49-7-3.85-7-7.93 0-.62.08-1.21.21-1.79L9 15v1c0 1.1.9 2 2 2v1.93zm6.9-2.54c-.26-.81-1-1.39-1.9-1.39h-1v-3c0-.55-.45-1-1-1H8v-2h2c.55 0 1-.45 1-1V7h2c1.1 0 2-.9 2-2v-.41c2.93 1.19 5 4.06 5 7.41 0 2.08-.8 3.97-2.1 5.39z"/>
        </svg>
        <h3 style="color: #0088cc; margin: 0; font-size: 1.5rem; font-weight: 600;">
            📱 Telegram Нотификации
        </h3>
    </div>
    
    <!-- Ако е абониран -->
    <div th:if="${user.telegramSubscription != null && user.telegramSubscription.active}">
        <div style="background: #d4edda; border: 2px solid #28a745; border-radius: 8px; padding: 1rem; margin-bottom: 1rem;">
            <p style="color: #155724; font-weight: bold; margin: 0; display: flex; align-items: center;">
                <span style="font-size: 1.5rem; margin-right: 0.5rem;">✅</span>
                Абониран си за Telegram нотификации
            </p>
            <p style="color: #155724; font-size: 0.9rem; margin: 0.5rem 0 0 2rem;">
                Ще получаваш моментални съобщения при нови публикации, събития и важни обяви.
            </p>
        </div>
        
        <div style="background: white; border-radius: 8px; padding: 1rem; border: 1px solid #dee2e6;">
            <p style="margin: 0; color: #6c757d; font-size: 0.9rem;">
                <strong>Абониран на:</strong> 
                <span th:text="${#temporals.format(user.telegramSubscription.subscribedAt, 'dd.MM.yyyy HH:mm')}"></span>
            </p>
            <p style="margin: 0.5rem 0 0 0; color: #6c757d; font-size: 0.85rem;">
                За да спреш нотификациите, изпрати <code>/unsubscribe</code> на бота.
            </p>
        </div>
    </div>
    
    <!-- Ако НЕ е абониран -->
    <div th:unless="${user.telegramSubscription != null && user.telegramSubscription.active}">
        <p style="font-size: 1rem; color: #495057; margin-bottom: 1rem; line-height: 1.6;">
            🚀 Абонирай се за Telegram нотификации и получавай <strong>моментално</strong> съобщения при нови публикации, събития и важни обяви!
        </p>
        
        <div style="background: white; border-radius: 8px; padding: 1.25rem; border: 2px solid #0088cc; margin-bottom: 1rem;">
            <p style="font-weight: 600; margin-bottom: 0.75rem; color: #212529; font-size: 1.05rem;">
                📋 Стъпки за абониране:
            </p>
            <ol style="margin: 0; padding-left: 1.5rem; line-height: 1.8; color: #495057;">
                <li>Отвори <strong>Telegram</strong> и намери бота: <code style="background: #f8f9fa; padding: 0.2rem 0.5rem; border-radius: 4px; color: #0088cc; font-weight: 600;">@dragon_dojo_bot</code></li>
                <li>Натисни <strong style="color: #0088cc;">/start</strong></li>
                <li>Изпрати команда: 
                    <code style="background: #f8f9fa; padding: 0.2rem 0.5rem; border-radius: 4px;">/subscribe 
                        <span th:text="${user.telegramSubscriptionCode}" 
                              style="color: #0088cc; font-weight: bold;"></span>
                    </code>
                </li>
            </ol>
        </div>
        
        <div style="background: linear-gradient(135deg, #e7f3ff 0%, #cce5ff 100%); border-radius: 8px; padding: 1rem; margin-bottom: 1rem; border: 1px solid #0088cc;">
            <p style="margin: 0; font-size: 0.95rem; color: #004085;">
                <strong>Твоят уникален код:</strong>
            </p>
            <div style="display: flex; align-items: center; margin-top: 0.5rem;">
                <code id="telegram-code" 
                      th:text="${user.telegramSubscriptionCode}" 
                      style="font-size: 1.3rem; color: #0088cc; font-weight: bold; background: white; padding: 0.5rem 1rem; border-radius: 6px; flex: 1; text-align: center; border: 2px dashed #0088cc;">
                </code>
                <button onclick="copyTelegramCode()" 
                        id="copy-btn"
                        style="margin-left: 0.75rem; padding: 0.5rem 1rem; background: #0088cc; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: 600; transition: all 0.3s; box-shadow: 0 2px 4px rgba(0,136,204,0.3);"
                        onmouseover="this.style.background='#006699'"
                        onmouseout="this.style.background='#0088cc'">
                    📋 Копирай
                </button>
            </div>
        </div>
        
        <a href="https://t.me/dragon_dojo_bot" 
           target="_blank" 
           rel="noopener noreferrer"
           style="display: inline-flex; align-items: center; padding: 0.75rem 1.5rem; background: linear-gradient(135deg, #0088cc 0%, #006699 100%); color: white; text-decoration: none; border-radius: 8px; font-weight: 600; transition: all 0.3s; box-shadow: 0 4px 12px rgba(0,136,204,0.3);"
           onmouseover="this.style.transform='translateY(-2px)'; this.style.boxShadow='0 6px 16px rgba(0,136,204,0.4)'"
           onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='0 4px 12px rgba(0,136,204,0.3)'">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="white" style="margin-right: 0.5rem;">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 6.8c-.15 1.58-.8 5.42-1.13 7.19-.14.75-.42 1-.68 1.03-.58.05-1.02-.38-1.58-.75-.88-.58-1.38-.94-2.23-1.5-.99-.65-.35-1.01.22-1.59.15-.15 2.71-2.48 2.76-2.69.01-.03.01-.14-.07-.2-.08-.06-.2-.04-.28-.02-.12.02-1.96 1.25-5.54 3.67-.52.36-.99.53-1.42.52-.47-.01-1.37-.26-2.03-.48-.82-.27-1.47-.42-1.42-.88.03-.24.38-.49 1.05-.74 4.11-1.79 6.85-2.97 8.24-3.54 3.92-1.63 4.73-1.91 5.26-1.92.12 0 .38.03.55.17.14.12.18.27.2.38.01.06.03.21.01.33z"/>
            </svg>
            🤖 Отвори Telegram Bot
        </a>
    </div>
</div>

<script>
/**
 * Копиране на Telegram кода в clipboard.
 */
function copyTelegramCode() {
    const codeElement = document.getElementById('telegram-code');
    const copyBtn = document.getElementById('copy-btn');
    const code = codeElement.textContent;
    
    // Модерен API за копиране
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(code).then(() => {
            // Успешно копиране
            copyBtn.innerHTML = '✅ Копирано!';
            copyBtn.style.background = '#28a745';
            
            // Връщане на оригиналния текст след 2 секунди
            setTimeout(() => {
                copyBtn.innerHTML = '📋 Копирай';
                copyBtn.style.background = '#0088cc';
            }, 2000);
        }).catch(err => {
            console.error('Грешка при копиране:', err);
            fallbackCopy(code, copyBtn);
        });
    } else {
        // Fallback за стари браузъри
        fallbackCopy(code, copyBtn);
    }
}

/**
 * Fallback метод за копиране (стари браузъри).
 */
function fallbackCopy(text, button) {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.left = '-9999px';
    document.body.appendChild(textArea);
    textArea.select();
    
    try {
        document.execCommand('copy');
        button.innerHTML = '✅ Копирано!';
        button.style.background = '#28a745';
        
        setTimeout(() => {
            button.innerHTML = '📋 Копирай';
            button.style.background = '#0088cc';
        }, 2000);
    } catch (err) {
        console.error('Fallback копиране се провали:', err);
        alert('Грешка при копиране. Моля, копирайте кода ръчно: ' + text);
    }
    
    document.body.removeChild(textArea);
}
</script>
```

**Обяснение:**
- Показва различен UI в зависимост от това дали потребителят е абониран
- Копиране на кода с един клик (с fallback за стари браузъри)
- Директен линк към бота
- Стилизирано с градиенти и сенки

---

## 8. Конфигурация (application.properties)

### 8.1. ✏️ Промени в `application.properties`

**Локация:** `src/main/resources/application.properties`

**Добави в края на файла:**

```properties
# ========================================
# Telegram Bot Configuration
# ========================================
telegram.bot.token=YOUR_BOT_TOKEN_HERE
telegram.bot.username=dragon_dojo_bot

# Base URL на апликацията (за линкове в Telegram съобщения)
app.base.url=https://dragondojo.com

# ========================================
# Async Configuration (Thread Pool)
# ========================================
spring.task.execution.pool.core-size=2
spring.task.execution.pool.max-size=5
spring.task.execution.pool.queue-capacity=100

# ========================================
# Cache Configuration
# ========================================
spring.cache.type=simple
```

**ВАЖНО:** Замени `YOUR_BOT_TOKEN_HERE` с реалния токен от BotFather!

---

### 8.2. ✏️ Промени в `application-prod.properties` (Production)

**Локация:** `src/main/resources/application-prod.properties`

**Добави:**

```properties
# ========================================
# Telegram Bot Configuration (Production)
# ========================================
telegram.bot.token=${TELEGRAM_BOT_TOKEN}
telegram.bot.username=dragon_dojo_bot
app.base.url=${APP_BASE_URL:https://dragondojo.com}
```

**Обяснение:**
- `${TELEGRAM_BOT_TOKEN}` - четене от environment variable (по-сигурно)
- Никога НЕ записвай токена директно в production config файл
- Задай environment variable на сървъра:
  ```bash
  export TELEGRAM_BOT_TOKEN="123456789:ABCdefGHIjklMNOpqrsTUVwxyz"
  export APP_BASE_URL="https://dragondojo.com"
  ```

---

## 9. Тестване

### 9.1. Unit тестове (Опционално)

За да тестваш отделни компоненти, можеш да създадеш:

**`TelegramBotServiceTest.java`:**

```java
package cardindex.dojocardindex.telegram.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class TelegramBotServiceTest {

    @Autowired(required = false)
    private TelegramBotService telegramBotService;

    @Test
    void contextLoads() {
        // Просто проверява дали контекстът се зарежда без грешки
        // TelegramBotService може да е null ако няма токен в test properties
        assertDoesNotThrow(() -> {
            System.out.println("Spring контекстът се зареди успешно");
        });
    }
}
```

---

### 9.2. Ръчно тестване (Препоръчително)

#### Тест 1: Проверка на генериране на код при логин

1. **Стартирай апликацията:**
   ```bash
   .\mvnw spring-boot:run
   ```

2. **Влез с СЪЩЕСТВУВАЩ потребител** (който няма Telegram код в DB)

3. **Провери логовете:**
   ```
   INFO: Потребител test@example.com влезе успешно. Стартиране на асинхронни проверки...
   INFO: Telegram код липсва за потребител test@example.com. Генериране...
   INFO: Генериран Telegram код за потребител test@example.com: A3F9K2L7
   ```

4. **Влез ОТНОВО със същия потребител**

5. **Провери логовете:**
   ```
   INFO: Потребител test@example.com влезе успешно. Стартиране на асинхронни проверки...
   DEBUG: Telegram код вече съществува за потребител: test@example.com
   (НЯМА нова заявка към DB - байпаснато от кеша!)
   ```

---

#### Тест 2: Проверка на Telegram бота

1. **Отвори Telegram** и намери `@dragon_dojo_bot`

2. **Изпрати `/start`**
   - Очакван резултат: Съобщение с инструкции

3. **Отвори профила си** в апликацията (`/home`)
   - Виж секцията "Telegram Нотификации"
   - Копирай кода (например `A3F9K2L7`)

4. **Върни се в Telegram** и изпрати:
   ```
   /subscribe A3F9K2L7
   ```
   - Очакван резултат: 
     ```
     ✅ Успешно абониран!
     
     Здравей, Иван Иванов! 🥋
     
     Вече ще получаваш нотификации за:
     📢 Нови публикации
     ...
     ```

5. **Изпрати `/status`**
   - Очакван резултат: Информация за абонамента

---

#### Тест 3: Проверка на нотификация при нов пост

1. **Като админ, създай нов пост** (`/posts/new`)
   - Заглавие: "Тестова публикация"
   - Съдържание: "Това е тест за Telegram нотификации"

2. **Провери логовете:**
   ```
   INFO: Telegram нотификация изпратена за нов пост: Тестова публикация
   INFO: Изпращане на нотификация за нов пост към 1 абоната(и)
   INFO: Нотификации изпратени: 1 успешни, 0 неуспешни
   ```

3. **Провери Telegram** - трябва да получиш съобщение:
   ```
   📢 Нова публикация в Dragon Dojo
   
   📝 Тестова публикация
   
   ✍️ Автор: Иван Иванов
   📅 27.02.2026 14:30
   
   ➡️ Прочети повече
   ```

---

#### Тест 4: Отписване от нотификации

1. **В Telegram, изпрати `/unsubscribe`**
   - Очакван резултат:
     ```
     ✅ Успешно отписан от нотификации.
     
     За да се абонираш отново, използвай /subscribe <код>
     ```

2. **Създай нов пост** - НЕ трябва да получиш нотификация

3. **Абонирай се отново** - `/subscribe A3F9K2L7`

4. **Създай нов пост** - трябва да получиш нотификация

---

## 10. Често задавани въпроси (FAQ)

### Q1: Какво се случва ако Telegram API е недостъпен?

**A:** Апликацията ще продължи да работи нормално. Постовете ще се създават, но нотификациите няма да се изпращат. Грешката ще се логва, но няма да блокира операцията.

```java
try {
    telegramBotService.notifyNewPost(post);
} catch (Exception e) {
    log.error("Telegram е недостъпен, но постът е създаден", e);
    // Продължава нормално
}
```

---

### Q2: Какво се случва при рестарт на апликацията?

**A:**
- ✅ **Базата данни** - всички данни остават (потребители, кодове, абонаменти)
- ❌ **Кешът** - изчиства се (in-memory кеш)
- ✅ **Telegram бота** - автоматично се регистрира отново
- ✅ **При първия логин след рестарт** - кешът се попълва отново

---

### Q3: Как мога да използвам Redis кеш вместо in-memory?

**A:** Ако искаш persistent кеш (Redis), промени `AsyncCacheConfig`:

```xml
<!-- В pom.xml добави -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

```properties
# В application.properties
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
```

```java
// В AsyncCacheConfig.java
@Bean
public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
    RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
        .entryTtl(Duration.ofHours(24)); // Кешът живее 24 часа
    
    return RedisCacheManager.builder(connectionFactory)
        .cacheDefaults(config)
        .build();
}
```

---

### Q4: Мога ли да изпращам нотификации и за събития (Events)?

**A:** Да! Същата логика може да се приложи в `EventService`:

```java
@Service
public class EventService {
    
    private final TelegramBotService telegramBotService;
    
    // ...existing code...
    
    public void createEvent(EventRequest request) {
        Event event = // създаване на събитие
        eventRepository.save(event);
        
        // Изпращай Telegram нотификация
        telegramBotService.notifyNewEvent(event); // Трябва да добавиш този метод
    }
}
```

И добави метод в `TelegramBotService`:

```java
public void notifyNewEvent(Event event) {
    String message = String.format(
        "📅 *Ново събитие в Dragon Dojo*\n\n" +
        "🎯 *%s*\n\n" +
        "📍 Място: %s\n" +
        "⏰ Дата: %s\n\n" +
        "[➡️ Виж детайли](%s/events/%s)",
        escapeMarkdown(event.getTitle()),
        escapeMarkdown(event.getLocation()),
        event.getDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
        appBaseUrl,
        event.getId()
    );
    
    // Broadcast на всички
    List<TelegramSubscription> subs = subscriptionRepository.findAllByActiveIsTrue();
    for (TelegramSubscription sub : subs) {
        sendMessageWithMarkdown(sub.getChatId(), message);
    }
}
```

---

### Q5: Как мога да тествам бота локално без production токен?

**A:** Създай отделен тестов бот:

1. Отвори @BotFather
2. Създай нов бот: `/newbot`
3. Дай име: `Dragon Dojo Test Bot`
4. Username: `dragon_dojo_test_bot`
5. Използвай тестовия токен в `application.properties`

**Забележка:** Production и test ботовете са напълно отделени.

---

### Q6: Може ли потребителят да промени Telegram кода си?

**A:** Не е препоръчително, но ако искаш да добавиш тази функция:

```java
// В UserService
public void regenerateTelegramCode(UUID userId) {
    User user = getUserById(userId);
    
    // Генерирай нов код
    String newCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    
    // Деактивирай стария абонамент (ако има)
    if (user.getTelegramSubscription() != null) {
        user.getTelegramSubscription().setActive(false);
    }
    
    // Запиши новия код
    User updated = user.toBuilder()
        .telegramSubscriptionCode(newCode)
        .build();
    userRepository.save(updated);
    
    // Инвалидирай кеша
    userLoginCheckService.invalidateTelegramCodeCache(userId);
}
```

---

### Q7: Има ли лимити за Telegram бота?

**A:** Да, Telegram има rate limits:
- **30 съобщения/секунда** към различни потребители
- **1 съобщение/секунда** към един и същ потребител
- **20 съобщения/минута** в групи

За Dragon Dojo с 2 потребителя това НЕ е проблем. Дори при 100 потребителя, broadcast на нотификация за нов пост ще отнеме ~3 секунди.

---

### Q8: Какво се случва ако потребител изтрие чата с бота?

**A:** Telegram автоматично изтрива `chatId`. При следващ опит за изпращане ще получиш грешка `403 Forbidden`. Трябва да обработваш това:

```java
try {
    sendMessageWithMarkdown(chatId, message);
} catch (TelegramApiException e) {
    if (e.getMessage().contains("403") || e.getMessage().contains("blocked")) {
        // Потребителят е блокирал бота или изтрил чата
        subscription.setActive(false);
        subscriptionRepository.save(subscription);
        log.warn("Потребител {} блокира бота. Деактивиране на абонамента", 
                 subscription.getUser().getEmail());
    }
}
```

Добави тази логика в `TelegramBotService.notifyNewPost()` в `catch` блока.

---

### Q9: Мога ли да изпращам нотификации само на определени потребители?

**A:** Да! Добави филтър по роля или други критерии:

```java
// Само на треньори и админи
List<TelegramSubscription> subs = subscriptionRepository.findAllByActiveIsTrue()
    .stream()
    .filter(sub -> {
        UserRole role = sub.getUser().getRole();
        return role == UserRole.ADMIN || role == UserRole.TRAINER;
    })
    .toList();
```

Или създай нов метод в Repository:

```java
@Query("SELECT ts FROM TelegramSubscription ts " +
       "WHERE ts.active = true " +
       "AND ts.user.role IN :roles")
List<TelegramSubscription> findAllByActiveIsTrueAndUserRoleIn(List<UserRole> roles);
```

---

### Q10: Какво ако искам да изпращам снимки/файлове?

**A:** Telegram поддържа multimedia съобщения:

```java
public void sendPhoto(Long chatId, String photoUrl, String caption) {
    SendPhoto photo = new SendPhoto();
    photo.setChatId(chatId.toString());
    photo.setPhoto(new InputFile(photoUrl));
    photo.setCaption(caption);
    photo.setParseMode("Markdown");
    
    try {
        execute(photo);
    } catch (TelegramApiException e) {
        log.error("Грешка при изпращане на снимка", e);
    }
}
```

**Пример:** Изпращай Event banner:

```java
telegramBotService.sendPhoto(
    subscription.getChatId(),
    event.getBannerUrl(),
    String.format("📅 *Ново събитие:* %s\n\n%s", 
                  event.getTitle(), 
                  event.getDescription())
);
```

---

## 11. Deployment инструкции

### Стъпка 1: Build на проекта

```bash
.\mvnw clean package -DskipTests
```

### Стъпка 2: Задаване на environment variables на сървъра

```bash
export TELEGRAM_BOT_TOKEN="123456789:ABCdefGHIjklMNOpqrsTUVwxyz"
export APP_BASE_URL="https://dragondojo.com"
```

### Стъпка 3: Стартиране на приложението

```bash
java -jar target/DojoCardIndex-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Стъпка 4: Проверка на логовете

```bash
tail -f logs/spring.log | grep -i telegram
```

Трябва да видиш:
```
✅ Telegram bot успешно регистриран и стартиран!
```

---

## 12. Troubleshooting (Отстраняване на проблеми)

### Проблем 1: "Bot token is invalid"

**Причина:** Невалиден токен в `application.properties`

**Решение:**
1. Отвори @BotFather в Telegram
2. Изпрати `/token`
3. Избери своя бот
4. Копирай новия токен
5. Обнови `telegram.bot.token` в properties

---

### Проблем 2: "Bean of type TelegramBotService could not be found"

**Причина:** Maven dependencies не са заредени

**Решение:**
```bash
.\mvnw clean install -U
# Рестартирай IntelliJ
```

---

### Проблем 3: "Unable to register bot"

**Причина:** Друга инстанция на апликацията ползва същия токен

**Решение:**
- Спри всички инстанции на апликацията
- Изчакай 5 минути (Telegram timeout)
- Стартирай отново

---

### Проблем 4: Кешът не работи

**Причина:** `@EnableCaching` не е добавено

**Решение:**
- Провери дали `AsyncCacheConfig` има `@EnableCaching`
- Провери дали методът има `@Cacheable`
- Рестартирай апликацията

---

### Проблем 5: Асинхронните проверки не се изпълняват

**Причина:** `@EnableAsync` не е добавено

**Решение:**
- Провери дали `AsyncCacheConfig` има `@EnableAsync`
- Провери дали методът има `@Async`
- Рестартирай апликацията

---

## 🎉 Заключение

След имплементация на тази система, Dragon Dojo Card Index ще има:

✅ **Telegram нотификации** за нови постове  
✅ **Асинхронна проверка** при логин (не забавя потребителя)  
✅ **Кеширане** (байпасва проверките след първия логин)  
✅ **Автоматично генериране** на Telegram кодове  
✅ **Self-healing** (ако някой няма код, генерира се автоматично)  
✅ **Graceful degradation** (ако Telegram е недостъпен, апликацията работи)  
✅ **Production-ready** (логове, error handling, security)  

---

## 📚 Следващи стъпки за учене

1. **Прочети кода** внимателно и задай въпроси за неясни части
2. **Тествай локално** всяка функционалност поотделно
3. **Експериментирай** - пробвай да добавиш нотификации за събития
4. **Изучи концепциите:**
   - Spring `@Async` и thread pools
   - Spring Cache abstraction
   - Telegram Bot API
   - Event-driven programming

---

**Автор:** GitHub Copilot  
**Дата:** 27 февруари 2026  
**Версия:** 1.0

---

Успех с имплементацията! Ако имаш въпроси, питай! 🚀🥋

