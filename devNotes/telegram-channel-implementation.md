# 📢 Telegram Канал/Група Нотификации - Проста Имплементация

**Дата:** 27 февруари 2026  
**Цел:** Изпращане на нотификации за нови публикации и събития в Telegram канал/група  
**Сложност:** ⭐ Лесна (1-2 часа)

---

## 📋 Съдържание

1. [Защо Telegram Канал вместо Bot Subscriptions](#1-защо-telegram-канал-вместо-bot-subscriptions)
2. [Как работи системата](#2-как-работи-системата)
3. [Подготовка - Създаване на Telegram Bot](#3-подготовка---създаване-на-telegram-bot)
4. [Създаване на Telegram Канал или Група](#4-създаване-на-telegram-канал-или-група)
5. [Вземане на Chat ID](#5-вземане-на-chat-id)
6. [Промени в зависимостите (pom.xml)](#6-промени-в-зависимостите-pomxml)
7. [Java имплементация](#7-java-имплементация)
8. [Промени в съществуващи класове](#8-промени-в-съществуващи-класове)
9. [Конфигурация (application.properties)](#9-конфигурация-applicationproperties)
10. [Тестване](#10-тестване)
11. [Frontend промени (опционално)](#11-frontend-промени-опционално)
12. [Често задавани въпроси (FAQ)](#12-често-задавани-въпроси-faq)
13. [Troubleshooting](#13-troubleshooting)

---

## 1. Защо Telegram Канал вместо Bot Subscriptions?

### 🎯 Сравнение на двата подхода:

| Критерий | Subscription Bot | Telegram Канал/Група ⭐ |
|----------|------------------|------------------------|
| **Код (брой редове)** | ~800+ | ~150 |
| **Java класове** | 5+ нови | **1 нов** |
| **База данни промени** | 2 таблици + миграции | **0 промени** |
| **Генериране на кодове** | Да (сложна логика) | **Не е нужно** |
| **Асинхронни проверки** | Да (при всеки логин) | **Не е нужно** |
| **Кеширане** | Да (Redis/In-Memory) | **Не е нужно** |
| **Конфигурация** | Много properties | **2 реда** |
| **Поддръжка** | Средна сложност | **Минимална** |
| **Onboarding на потребител** | Копиране на код + /subscribe | **1 клик (Join)** |
| **Ресурси (CPU/Memory)** | Средни | **Минимални** |
| **Telegram API заявки** | N (брой потребители) | **1 заявка** |
| **Rate limits риск** | Да (при >30 потребители/сек) | **Не** |
| **Време за имплементация** | 4-6 часа | **1-2 часа** |

### ✅ Предимства на Telegram Канал:

1. **Супер проста имплементация** - 1 клас, ~150 реда код
2. **Нулеви база данни промени** - не пипаме User модела
3. **Нулева сложност** - няма subscriptions, кодове, кеширане
4. **Едно съобщение → всички го виждат** моментално
5. **Потребителите просто се join-ват** с 1 клик
6. **Telegram управлява членството** - няма ръчна работа
7. **Broadcast нотификации** - перфектно за обяви
8. **Работи веднага** - без сложна логика

### ⚠️ Недостатъци:

1. ❌ Няма персонализация (всички виждат всичко)
2. ❌ Не можеш да филтрираш по роля
3. ❌ Няма статистика кой е видял какво
4. ❌ Всички или никой (не можеш да изпратиш само на треньори)

---

## 2. Как работи системата

### 📊 Визуална диаграма:

```
┌─────────────────────────────────────────────────────────────────────────┐
│  СТЪПКА 1: ПОДГОТОВКА (Еднократно)                                     │
│                                                                         │
│  1. Създаваш Telegram Бот чрез @BotFather                              │
│     └─> Получаваш токен: 123456789:ABCdefGHIjklMNOpqrsTUVwxyz         │
│                                                                         │
│  2. Създаваш Telegram Канал/Група "Dragon Dojo Обяви" 🥋               │
│     └─> Public: t.me/dragon_dojo_announcements                         │
│     └─> Private: Invite link                                           │
│                                                                         │
│  3. Добавяш бота като администратор в канала                           │
│     └─> Даваш му права да пише съобщения                               │
│                                                                         │
│  4. Вземаш Chat ID на канала/групата                                   │
│     └─> Например: -1001234567890                                       │
│     └─> Записваш в application.properties                              │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  СТЪПКА 2: ONBOARDING НА ПОТРЕБИТЕЛИ                                   │
│                                                                         │
│  Вариант А (Public канал):                                             │
│  └─> Споделяш линк: t.me/dragon_dojo_announcements                     │
│      └─> Потребителите кликат → Join → Готово! ✅                      │
│                                                                         │
│  Вариант Б (Private канал):                                            │
│  └─> Генерираш invite link в канала                                    │
│      └─> Споделяш го на потребителите                                  │
│      └─> Те кликат → Join → Готово! ✅                                 │
│                                                                         │
│  Вариант В (В приложението):                                           │
│  └─> Добавяш бутон в user-details.html:                                │
│      "🔔 Абонирай се за Telegram обяви"                                │
│      └─> Води към канала                                               │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  СТЪПКА 3: ИЗПРАЩАНЕ НА НОТИФИКАЦИЯ (Автоматично)                     │
│                                                                         │
│  Админът създава нов пост:                                             │
│  └─> PostController.createPost()                                       │
│      └─> PostService.createNewPost()                                   │
│          └─> Записва в DB ✅                                           │
│          └─> TelegramNotificationService.notifyNewPost(post)           │
│              └─> Изгражда съобщение 📝                                 │
│              └─> Изпраща в канала 📢                                   │
│                  └─> SendMessage.chatId = -1001234567890              │
│                      └─> execute(message)                              │
│                          └─> 1 API заявка → Telegram                  │
│                              └─> Всички членове получават нотификация!│
│                                  🎉🎉🎉                                 │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│  РЕЗУЛТАТ: Потребителите виждат в Telegram                             │
│                                                                         │
│  Dragon Dojo Обяви 🥋                                                   │
│  ─────────────────────────────────────────────────────────────         │
│  📢 Нова публикация в Dragon Dojo                                      │
│                                                                         │
│  📝 Нов график за тренировки през март                                 │
│                                                                         │
│  ✍️ Автор: Иван Петров                                                 │
│  📅 27.02.2026 14:30                                                    │
│                                                                         │
│  ➡️ Прочети повече                                                     │
│  [Link към пълната публикация]                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Подготовка - Създаване на Telegram Bot

### Стъпка 1: Регистриране на бота в Telegram

1. **Отвори Telegram** и намери **@BotFather**
2. Изпрати команда: `/newbot`
3. BotFather ще те попита за информация:

```
BotFather: Alright, a new bot. How are we going to call it? 
           Please choose a name for your bot.

Ти: Dragon Dojo Notifications Bot

BotFather: Good. Now let's choose a username for your bot. 
           It must end in `bot`. Like this, for example: TetrisBot or tetris_bot.

Ти: dragon_dojo_notifications_bot

BotFather: Done! Congratulations on your new bot. 
           You will find it at t.me/dragon_dojo_notifications_bot. 
           You can now add a description, about section and profile picture 
           for your bot, see /help for a list of commands.

           Use this token to access the HTTP API:
           123456789:ABCdefGHIjklMNOpqrsTUVwxyz1234567

           Keep your token secure and store it safely, 
           it can be used by anyone to control your bot.
```

4. **ВАЖНО:** Запази токена! Ще ти трябва за конфигурацията.

### Стъпка 2: Персонализиране на бота (опционално, но препоръчително)

```
/setdescription - Добави описание на бота
```

**Пример за описание:**
```
Официален бот на Dragon Dojo за автоматични нотификации при нови публикации, събития и важни обяви. 🥋
```

```
/setabouttext - Добави "About" текст
```

**Пример:**
```
Бот за автоматични нотификации на Dragon Dojo Karate Club.
```

```
/setuserpic - Качи профилна снимка
```
- Качи логото на Dragon Dojo (dragon_blue_logo.svg конвертирано в PNG/JPG)

---

## 4. Създаване на Telegram Канал или Група

### Опция А: **Telegram Канал** (Препоръчвам) ⭐

**Защо канал:**
- ✅ Само админи пишат → няма spam от потребители
- ✅ По-чисто и професионално
- ✅ Потребителите само четат обяви
- ✅ Показва брой абонати
- ✅ Може да се направи public с custom username

**Стъпки:**

1. **Отвори Telegram** → Menu → **New Channel**

2. **Въведи име:**
   ```
   Име: Dragon Dojo Обяви 🥋
   ```

3. **Въведи описание:**
   ```
   Официални обяви, нови публикации и събития от Dragon Dojo Karate Club.
   Следвай пътя на каратето! 🥋
   ```

4. **Избери тип:**
   
   **Public Channel** (препоръчително):
   - Username: `dragon_dojo_announcements`
   - Link: `t.me/dragon_dojo_announcements`
   - Всеки може да се join-не с този линк
   
   **Private Channel**:
   - Няма public username
   - Генерираш invite link
   - Споделяш линка само на желаните хора

5. **Skip добавяне на членове** (ще го направим по-късно)

6. **Настройки на канала:**
   - Отвори канала → **⋮** (Menu) → **Manage Channel**
   - **Administrators** → **Add Administrator** → Намери `@dragon_dojo_notifications_bot`
   - **Дай права:**
     - ✅ **Post Messages** (задължително!)
     - ✅ **Edit Messages of Others** (опционално)
     - ❌ Останалите права не са нужни
   - **Save**

---

### Опция Б: **Telegram Група**

**Защо група:**
- ✅ Потребителите могат да коментират
- ✅ По-интерактивно
- ✅ Дискусии под обявите
- ❌ Може да има spam
- ❌ По-трудно за модериране

**Стъпки:**

1. **Отвори Telegram** → Menu → **New Group**

2. **Въведи име:**
   ```
   Dragon Dojo Обяви 🥋
   ```

3. **Добави първоначални членове** (опционално, можеш да skip-неш)

4. **Upgrade на Supergroup** (ако групата е обикновена):
   - Settings → Group Type → **Supergroup**

5. **Настройки на групата:**
   - Отвори групата → **⋮** (Menu) → **Manage Group**
   - **Administrators** → **Add Administrator** → Намери `@dragon_dojo_notifications_bot`
   - **Дай права:**
     - ✅ **Send Messages** (задължително!)
     - ✅ **Delete Messages** (опционално)
     - ❌ Останалите права не са нужни
   - **Save**

6. **Настройки за пермишъни (опционално):**
   - **Permissions** → **Send Messages**: Only admins
   - По този начин само админи (включително бота) могат да пишат
   - Потребителите само четат (като в канал)

---

## 5. Вземане на Chat ID

**Chat ID** е уникалният идентификатор на канала/групата. Изглежда така: `-1001234567890`

### Метод 1: Чрез Telegram API (Лесен)

1. **Изпрати произволно съобщение** в канала/групата (като админ)
   - Пример: "Тест 123"

2. **Отвори браузър** и отиди на:
   ```
   https://api.telegram.org/bot<YOUR_BOT_TOKEN>/getUpdates
   ```
   
   Замени `<YOUR_BOT_TOKEN>` с твоя токен от BotFather.
   
   **Пример:**
   ```
   https://api.telegram.org/bot123456789:ABCdefGHIjklMNOpqrsTUVwxyz/getUpdates
   ```

3. **Виж JSON отговора** и намери:
   ```json
   {
     "ok": true,
     "result": [
       {
         "update_id": 123456789,
         "message": {
           "message_id": 2,
           "from": {...},
           "chat": {
             "id": -1001234567890,    ← ТОВА Е CHAT ID!
             "title": "Dragon Dojo Обяви",
             "type": "channel"
           },
           "date": 1709044800,
           "text": "Тест 123"
         }
       }
     ]
   }
   ```

4. **Копирай Chat ID:** `-1001234567890`

**Забележка:** Chat ID на канали/групи **винаги започва с минус** (-)!

---

### Метод 2: Чрез Bot (Програмно)

Ако горният метод не работи, можеш да добавиш временен endpoint:

**1. Създай helper клас:**

```java
package cardindex.dojocardindex.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.updates.GetUpdates;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@RestController
@Slf4j
public class TelegramDebugController {

    private final String botToken;

    public TelegramDebugController(@Value("${telegram.bot.token}") String botToken) {
        this.botToken = botToken;
    }

    /**
     * ВРЕМЕНЕН endpoint за вземане на Chat ID.
     * Използвай го само веднъж, после го изтрий!
     * 
     * Стъпки:
     * 1. Изпрати съобщение в канала/групата
     * 2. Отвори: http://localhost:8080/telegram/debug/get-chat-id
     * 3. Виж Chat ID в отговора
     * 4. Изтрий този клас
     */
    @GetMapping("/telegram/debug/get-chat-id")
    public String getChatId() {
        DefaultAbsSender sender = new DefaultAbsSender(new DefaultBotOptions(), botToken) {
            @Override
            public String getBotToken() {
                return botToken;
            }
        };

        try {
            GetUpdates getUpdates = new GetUpdates();
            getUpdates.setLimit(10);
            getUpdates.setOffset(-1);
            
            List<Update> updates = sender.execute(getUpdates);
            
            StringBuilder result = new StringBuilder("Telegram Updates:\n\n");
            
            for (Update update : updates) {
                if (update.hasMessage()) {
                    Long chatId = update.getMessage().getChatId();
                    String chatTitle = update.getMessage().getChat().getTitle();
                    String chatType = update.getMessage().getChat().getType();
                    
                    result.append("Chat ID: ").append(chatId).append("\n");
                    result.append("Chat Title: ").append(chatTitle).append("\n");
                    result.append("Chat Type: ").append(chatType).append("\n");
                    result.append("Message: ").append(update.getMessage().getText()).append("\n");
                    result.append("---\n\n");
                }
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Грешка при вземане на updates", e);
            return "Грешка: " + e.getMessage();
        }
    }
}
```

**2. Използвай endpoint-а:**

1. Стартирай апликацията
2. Изпрати съобщение в канала/групата
3. Отвори: `http://localhost:8080/telegram/debug/get-chat-id`
4. Виж Chat ID в отговора
5. **ВАЖНО: Изтрий този клас след като вземеш Chat ID!**

---

### Метод 3: Чрез специализиран бот (Най-лесен)

1. Намери бота **@getidsbot** в Telegram
2. Препрати му съобщение от канала/групата
3. Ботът ще ти отговори с Chat ID

или

1. Добави бота **@raw_data_bot** в групата/канала
2. Изпрати съобщение
3. Ботът ще отговори с JSON, включително Chat ID

---

## 6. Промени в зависимостите (pom.xml)

### Добави Telegram Bot API dependency:

**Локация:** `pom.xml`

```xml
<dependencies>
    <!-- ...existing dependencies... -->

    <!-- Telegram Bot API -->
    <dependency>
        <groupId>org.telegram</groupId>
        <artifactId>telegrambots</artifactId>
        <version>6.9.7.1</version>
    </dependency>

    <!-- ...existing dependencies... -->
</dependencies>
```

### След промяната:

```bash
# В PowerShell терминала:
.\mvnw clean install
```

**Или** рестартирай IntelliJ и Maven ще download-не автоматично.

---

## 7. Java имплементация

### 7.1. 📁 `TelegramNotificationService.java` (ОСНОВНИЯТ КЛАС)

**Локация:** `src/main/java/cardindex/dojocardindex/telegram/TelegramNotificationService.java`

**Създай новата директория:**
```
src/main/java/cardindex/dojocardindex/
└── telegram/
    └── TelegramNotificationService.java
```

**Пълен код:**

```java
package cardindex.dojocardindex.telegram;

import cardindex.dojocardindex.Event.models.Event;
import cardindex.dojocardindex.Post.models.Post;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.format.DateTimeFormatter;

/**
 * Service за изпращане на нотификации в Telegram канал/група.
 * 
 * Това е СУПЕР ПРОСТАТА имплементация:
 * - 1 Chat ID → Всички членове на канала/групата получават нотификация
 * - Няма subscriptions, кодове, база данни промени
 * - Потребителите просто се join-ват в канала
 * 
 * @author Dragon Dojo Team
 */
@Service
@Slf4j
public class TelegramNotificationService extends DefaultAbsSender {

    /**
     * Telegram Bot Token (от BotFather)
     */
    @Value("${telegram.bot.token}")
    private String botToken;

    /**
     * Chat ID на канала/групата (например: -1001234567890)
     */
    @Value("${telegram.channel.chat.id}")
    private String channelChatId;

    /**
     * Base URL на апликацията (за линкове в съобщенията)
     */
    @Value("${app.base.url:https://dragondojo.com}")
    private String appBaseUrl;

    /**
     * Дали нотификациите са включени (може да се изключат временно)
     */
    @Value("${telegram.notifications.enabled:true}")
    private boolean notificationsEnabled;

    public TelegramNotificationService(@Value("${telegram.bot.token}") String botToken) {
        super(new DefaultBotOptions(), botToken);
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    /**
     * Изпращане на нотификация за нов пост в канала/групата.
     * 
     * @param post Новата публикация
     */
    public void notifyNewPost(Post post) {
        if (!notificationsEnabled) {
            log.info("Telegram нотификациите са изключени (telegram.notifications.enabled=false)");
            return;
        }

        String message = buildPostMessage(post);
        sendToChannel(message);
        
        log.info("📢 Telegram нотификация изпратена за пост: {}", post.getTitle());
    }

    /**
     * Изпращане на нотификация за ново събитие.
     * 
     * @param event Новото събитие
     */
    public void notifyNewEvent(Event event) {
        if (!notificationsEnabled) {
            log.info("Telegram нотификациите са изключени");
            return;
        }

        String message = buildEventMessage(event);
        sendToChannel(message);
        
        log.info("📅 Telegram нотификация изпратена за събитие: {}", event.getTitle());
    }

    /**
     * Изпращане на custom съобщение в канала.
     * Полезно за ръчни обяви от админ панел.
     * 
     * @param title Заглавие на обявата
     * @param content Съдържание
     */
    public void sendCustomAnnouncement(String title, String content) {
        if (!notificationsEnabled) {
            log.info("Telegram нотификациите са изключени");
            return;
        }

        String message = String.format(
            "📣 *%s*\n\n%s",
            escapeMarkdown(title),
            escapeMarkdown(content)
        );
        
        sendToChannel(message);
        log.info("📣 Custom обява изпратена: {}", title);
    }

    /**
     * Изпращане на снимка със съобщение (например Event banner).
     * 
     * @param photoUrl URL на снимката
     * @param caption Текст под снимката
     */
    public void sendPhotoWithCaption(String photoUrl, String caption) {
        if (!notificationsEnabled) {
            log.info("Telegram нотификациите са изключени");
            return;
        }

        SendPhoto photo = new SendPhoto();
        photo.setChatId(channelChatId);
        photo.setPhoto(new InputFile(photoUrl));
        photo.setCaption(caption);
        photo.setParseMode("Markdown");

        try {
            execute(photo);
            log.info("📸 Снимка изпратена в Telegram канала");
        } catch (TelegramApiException e) {
            log.error("❌ Грешка при изпращане на снимка в Telegram", e);
        }
    }

    /**
     * Изграждане на съобщение за нов пост.
     */
    private String buildPostMessage(Post post) {
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
     * Изграждане на съобщение за ново събитие.
     */
    private String buildEventMessage(Event event) {
        return String.format(
            "📅 *Ново събитие в Dragon Dojo*\n\n" +
            "🎯 *%s*\n\n" +
            "📍 Място: %s\n" +
            "⏰ %s\n\n" +
            "[➡️ Виж детайли](%s/events)",
            escapeMarkdown(event.getTitle()),
            escapeMarkdown(event.getLocation() != null ? event.getLocation() : "Не е указано"),
            event.getEventDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
            appBaseUrl
        );
    }

    /**
     * Основен метод за изпращане на текстово съобщение в канала/групата.
     * 
     * @param message Текст на съобщението (поддържа Markdown)
     */
    private void sendToChannel(String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(channelChatId);
        sendMessage.setText(message);
        sendMessage.setParseMode("Markdown");
        sendMessage.disableWebPagePreview();

        try {
            execute(sendMessage);
            log.debug("✅ Telegram нотификация изпратена успешно");
        } catch (TelegramApiException e) {
            log.error("❌ Грешка при изпращане на Telegram нотификация", e);
            log.error("Проверете:");
            log.error("  - telegram.bot.token в application.properties");
            log.error("  - telegram.channel.chat.id в application.properties");
            log.error("  - Дали ботът е администратор в канала/групата");
            log.error("  - Дали ботът има права за изпращане на съобщения");
        }
    }

    /**
     * Escape на специални символи за Telegram Markdown.
     * 
     * Telegram Markdown v2 изисква escape на специални символи,
     * иначе хвърля грешка при изпращане.
     * 
     * @param text Текст за escape
     * @return Escaped текст
     */
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        
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

    /**
     * Проверка дали Telegram нотификациите са достъпни.
     * Полезно за health check endpoints.
     * 
     * @return true ако са конфигурирани и работят
     */
    public boolean isAvailable() {
        return notificationsEnabled 
            && botToken != null 
            && !botToken.isEmpty() 
            && channelChatId != null 
            && !channelChatId.isEmpty();
    }

    /**
     * Тестово съобщение за проверка на конфигурацията.
     * Използвай го само при първоначално настройване!
     */
    public void sendTestMessage() {
        String testMessage = "✅ *Telegram нотификациите работят!*\n\n" +
                           "Това е тестово съобщение от Dragon Dojo системата.\n" +
                           "Ако виждаш това съобщение, всичко е настроено правилно! 🥋";
        sendToChannel(testMessage);
    }
}
```

---

## 8. Промени в съществуващи класове

### 8.1. ✏️ Промени в `PostService.java`

**Локация:** `src/main/java/cardindex/dojocardindex/Post/Service/PostService.java`

**Добави dependency injection:**

```java
package cardindex.dojocardindex.Post.Service;

// ...existing imports...
import cardindex.dojocardindex.telegram.TelegramNotificationService;

@Slf4j
@Service
public class PostService {

    private final PostRepository postRepository;
    private final UserService userService;
    private final TelegramNotificationService telegramNotificationService; // НОВО!

    @Autowired
    public PostService(PostRepository postRepository, 
                       UserService userService,
                       TelegramNotificationService telegramNotificationService) { // НОВО!
        this.postRepository = postRepository;
        this.userService = userService;
        this.telegramNotificationService = telegramNotificationService; // НОВО!
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
            telegramNotificationService.notifyNewPost(post);
            log.info("✅ Telegram нотификация изпратена за пост: {}", post.getTitle());
        } catch (Exception e) {
            log.error("❌ Грешка при изпращане на Telegram нотификация", e);
            // ВАЖНО: Не блокираме създаването на поста ако Telegram е недостъпен
        }
    }

    // ...existing methods...
}
```

**Обяснение:**
- Добавяме `TelegramNotificationService` в конструктора
- След записване на поста, извикваме `notifyNewPost()`
- Ако Telegram е недостъпен, грешката се логва, но постът се създава нормално (graceful degradation)

---

### 8.2. ✏️ Промени в `EventService.java` (Опционално)

Ако искаш нотификации и за нови събития:

**Локация:** `src/main/java/cardindex/dojocardindex/Event/service/EventService.java`

```java
package cardindex.dojocardindex.Event.service;

// ...existing imports...
import cardindex.dojocardindex.telegram.TelegramNotificationService;

@Service
@Slf4j
public class EventService {

    // ...existing fields...
    private final TelegramNotificationService telegramNotificationService; // НОВО!

    @Autowired
    public EventService(EventRepository eventRepository,
                        UserService userService,
                        // ...existing dependencies...
                        TelegramNotificationService telegramNotificationService) { // НОВО!
        // ...existing assignments...
        this.telegramNotificationService = telegramNotificationService; // НОВО!
    }

    // ...existing methods...

    public void createEvent(CreateEventRequest request) {
        // ...existing event creation logic...
        
        Event event = // създаване на събитие
        eventRepository.save(event);
        
        // НОВО: Изпращай Telegram нотификация
        try {
            telegramNotificationService.notifyNewEvent(event);
            log.info("✅ Telegram нотификация изпратена за събитие: {}", event.getTitle());
        } catch (Exception e) {
            log.error("❌ Грешка при изпращане на Telegram нотификация за събитие", e);
        }
    }

    // ...existing methods...
}
```

---

## 9. Конфигурация (application.properties)

### 9.1. ✏️ `application.properties` (Development)

**Локация:** `src/main/resources/application.properties`

**Добави в края на файла:**

```properties
# ========================================
# Telegram Notification Configuration
# ========================================

# Telegram Bot Token (от @BotFather)
telegram.bot.token=123456789:ABCdefGHIjklMNOpqrsTUVwxyz1234567

# Chat ID на Telegram канала/групата
# ВАЖНО: За канали/групи ID-то започва с минус (-)
# Пример: -1001234567890
telegram.channel.chat.id=-1001234567890

# Дали нотификациите са включени
# Задай false ако искаш временно да спреш нотификациите
telegram.notifications.enabled=true

# Base URL на апликацията (за линкове в Telegram съобщенията)
app.base.url=http://localhost:8080
```

**ВАЖНО:** 
- Замени `123456789:ABCdefGHIjklMNOpqrsTUVwxyz1234567` с реалния токен от BotFather
- Замени `-1001234567890` с реалния Chat ID на твоя канал/група

---

### 9.2. ✏️ `application-prod.properties` (Production)

**Локация:** `src/main/resources/application-prod.properties`

**Добави:**

```properties
# ========================================
# Telegram Notification Configuration (Production)
# ========================================

# Telegram Bot Token (от environment variable)
# ВАЖНО: Никога НЕ записвай токена директно в production config!
telegram.bot.token=${TELEGRAM_BOT_TOKEN}

# Chat ID на канала/групата
telegram.channel.chat.id=${TELEGRAM_CHANNEL_CHAT_ID}

# Нотификациите са включени в production
telegram.notifications.enabled=true

# Production URL
app.base.url=${APP_BASE_URL:https://dragondojo.com}
```

**Задаване на environment variables на production сървъра:**

```bash
# Linux/Mac
export TELEGRAM_BOT_TOKEN="123456789:ABCdefGHIjklMNOpqrsTUVwxyz"
export TELEGRAM_CHANNEL_CHAT_ID="-1001234567890"
export APP_BASE_URL="https://dragondojo.com"

# Windows PowerShell
$env:TELEGRAM_BOT_TOKEN="123456789:ABCdefGHIjklMNOpqrsTUVwxyz"
$env:TELEGRAM_CHANNEL_CHAT_ID="-1001234567890"
$env:APP_BASE_URL="https://dragondojo.com"
```

---

## 10. Тестване

### 10.1. Unit тестове (Опционално)

**Локация:** `src/test/java/cardindex/dojocardindex/telegram/TelegramNotificationServiceTest.java`

```java
package cardindex.dojocardindex.telegram;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "telegram.bot.token=test-token",
    "telegram.channel.chat.id=-100123456789",
    "telegram.notifications.enabled=false"
})
class TelegramNotificationServiceTest {

    @Autowired
    private TelegramNotificationService telegramNotificationService;

    @Test
    void contextLoads() {
        assertNotNull(telegramNotificationService);
    }

    @Test
    void isAvailableReturnsFalseWhenDisabled() {
        // Нотификациите са disabled в тестовите properties
        assertFalse(telegramNotificationService.isAvailable());
    }
}
```

---

### 10.2. Ръчно тестване (Препоръчително)

#### Тест 1: Проверка на конфигурацията

1. **Стартирай апликацията:**
   ```bash
   .\mvnw spring-boot:run
   ```

2. **Провери логовете:**
   ```
   INFO: Starting DojoCardIndexApplication...
   INFO: Started DojoCardIndexApplication in X.XXX seconds
   ```

3. **Ако има грешка в конфигурацията:**
   ```
   ERROR: ❌ Грешка при изпращане на Telegram нотификация
   ERROR: Проверете:
   ERROR:   - telegram.bot.token в application.properties
   ERROR:   - telegram.channel.chat.id в application.properties
   ```

---

#### Тест 2: Изпращане на тестово съобщение

**Вариант А: Чрез Debug Controller (Временен)**

Създай временен controller:

```java
package cardindex.dojocardindex.telegram;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TelegramTestController {

    @Autowired
    private TelegramNotificationService telegramNotificationService;

    /**
     * ВРЕМЕНЕН endpoint за тестване.
     * Отвори: http://localhost:8080/telegram/test
     * ИЗТРИЙ след тестване!
     */
    @GetMapping("/telegram/test")
    public String sendTest() {
        try {
            telegramNotificationService.sendTestMessage();
            return "✅ Тестово съобщение изпратено! Провери канала/групата в Telegram.";
        } catch (Exception e) {
            return "❌ Грешка: " + e.getMessage();
        }
    }
}
```

**Използване:**
1. Отвори браузър: `http://localhost:8080/telegram/test`
2. Провери канала/групата в Telegram
3. Трябва да видиш: "✅ Telegram нотификациите работят!"
4. **ИЗТРИЙ `TelegramTestController` след теста!**

---

**Вариант Б: Чрез PostService (Реален тест)**

1. **Влез в апликацията** като админ
2. **Създай нова публикация** (`/posts/new`):
   - Заглавие: "Тестова публикация за Telegram"
   - Съдържание: "Това е тест за нотификациите"
3. **Натисни "Публикувай"**
4. **Провери логовете:**
   ```
   INFO: ✅ Telegram нотификация изпратена за пост: Тестова публикация за Telegram
   DEBUG: ✅ Telegram нотификация изпратена успешно
   ```
5. **Отвори Telegram** → Канала/Групата
6. **Трябва да видиш съобщение:**
   ```
   📢 Нова публикация в Dragon Dojo
   
   📝 Тестова публикация за Telegram
   
   ✍️ Автор: Иван Петров
   📅 27.02.2026 14:30
   
   ➡️ Прочети повече
   ```

---

#### Тест 3: Проверка на graceful degradation

**Цел:** Да се увериш, че апликацията работи дори ако Telegram е недостъпен.

1. **Спри интернет връзката** (временно)
2. **Създай нова публикация**
3. **Провери логовете:**
   ```
   ERROR: ❌ Грешка при изпращане на Telegram нотификация
   INFO: Post created successfully
   ```
4. **Потвърди:** Постът е създаден в базата данни ✅
5. **Включи интернета отново**
6. **Създай друга публикация** → сега нотификацията трябва да работи ✅

---

#### Тест 4: Join на потребители в канала

1. **Ако е Public канал:**
   - Отвори: `t.me/dragon_dojo_announcements`
   - Натисни **"Join"**
   - Готово! ✅

2. **Ако е Private канал:**
   - Отвори канала в Telegram (като админ)
   - Settings → Invite Link → Copy Link
   - Изпрати линка на потребителя
   - Потребителят кликва → Join → Готово! ✅

3. **Създай нова публикация**
4. **Потребителят трябва да получи нотификация** в Telegram! 🎉

---

## 11. Frontend промени (опционално)

### 11.1. ✏️ Добавяне на бутон в `user-details.html`

Можеш да добавиш бутон/линк в профилната страница, който води към Telegram канала:

**Локация:** `src/main/resources/templates/user-details.html`

**Добави след информацията на потребителя:**

```html
<!-- Telegram Канал секция -->
<div class="telegram-channel-section" 
     style="margin-top: 2rem; padding: 1.5rem; background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%); border-radius: 12px; border-left: 4px solid #0088cc; box-shadow: 0 2px 8px rgba(0,0,0,0.1);">
    
    <div style="display: flex; align-items: center; margin-bottom: 1rem;">
        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 24 24" fill="#0088cc" style="margin-right: 0.75rem;">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 6.8c-.15 1.58-.8 5.42-1.13 7.19-.14.75-.42 1-.68 1.03-.58.05-1.02-.38-1.58-.75-.88-.58-1.38-.94-2.23-1.5-.99-.65-.35-1.01.22-1.59.15-.15 2.71-2.48 2.76-2.69.01-.03.01-.14-.07-.2-.08-.06-.2-.04-.28-.02-.12.02-1.96 1.25-5.54 3.67-.52.36-.99.53-1.42.52-.47-.01-1.37-.26-2.03-.48-.82-.27-1.47-.42-1.42-.88.03-.24.38-.49 1.05-.74 4.11-1.79 6.85-2.97 8.24-3.54 3.92-1.63 4.73-1.91 5.26-1.92.12 0 .38.03.55.17.14.12.18.27.2.38.01.06.03.21.01.33z"/>
        </svg>
        <h3 style="color: #0088cc; margin: 0; font-size: 1.5rem; font-weight: 600;">
            📢 Telegram Обяви
        </h3>
    </div>
    
    <p style="font-size: 1rem; color: #495057; margin-bottom: 1rem; line-height: 1.6;">
        🚀 Абонирай се за нашия Telegram канал и получавай <strong>моментални</strong> нотификации за:
    </p>
    
    <ul style="margin: 0 0 1.5rem 1.5rem; line-height: 1.8; color: #495057;">
        <li>📢 <strong>Нови публикации</strong> в блога</li>
        <li>📅 <strong>Предстоящи събития</strong> и тренировки</li>
        <li>🏆 <strong>Важни обяви</strong> от треньорите</li>
        <li>🥋 <strong>Състезания и изпити</strong></li>
    </ul>
    
    <a href="https://t.me/dragon_dojo_announcements" 
       target="_blank" 
       rel="noopener noreferrer"
       style="display: inline-flex; align-items: center; padding: 0.75rem 1.5rem; background: linear-gradient(135deg, #0088cc 0%, #006699 100%); color: white; text-decoration: none; border-radius: 8px; font-weight: 600; transition: all 0.3s; box-shadow: 0 4px 12px rgba(0,136,204,0.3);"
       onmouseover="this.style.transform='translateY(-2px)'; this.style.boxShadow='0 6px 16px rgba(0,136,204,0.4)'"
       onmouseout="this.style.transform='translateY(0)'; this.style.boxShadow='0 4px 12px rgba(0,136,204,0.3)'">
        <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="white" style="margin-right: 0.5rem;">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm4.64 6.8c-.15 1.58-.8 5.42-1.13 7.19-.14.75-.42 1-.68 1.03-.58.05-1.02-.38-1.58-.75-.88-.58-1.38-.94-2.23-1.5-.99-.65-.35-1.01.22-1.59.15-.15 2.71-2.48 2.76-2.69.01-.03.01-.14-.07-.2-.08-.06-.2-.04-.28-.02-.12.02-1.96 1.25-5.54 3.67-.52.36-.99.53-1.42.52-.47-.01-1.37-.26-2.03-.48-.82-.27-1.47-.42-1.42-.88.03-.24.38-.49 1.05-.74 4.11-1.79 6.85-2.97 8.24-3.54 3.92-1.63 4.73-1.91 5.26-1.92.12 0 .38.03.55.17.14.12.18.27.2.38.01.06.03.21.01.33z"/>
        </svg>
        📱 Присъедини се към Telegram канала
    </a>
    
    <p style="margin: 1rem 0 0 0; font-size: 0.85rem; color: #6c757d;">
        💡 След като се присъединиш, ще получаваш автоматични нотификации директно на телефона си!
    </p>
</div>
```

**ВАЖНО:** Замени `https://t.me/dragon_dojo_announcements` с реалния линк на твоя канал!

---

### 11.2. ✏️ Добавяне на линк в навигацията (Опционално)

Ако искаш линк в главното меню:

**Локация:** `src/main/resources/templates/fragments/nav.html`

```html
<nav class="menu">
    <div></div>
    <div>
        <ul>
            <li id="home-link"><a th:href="@{/}">Начало</a></li>
            <li><a th:href="@{/about-us}">За нас</a></li>
            <li sec:authorize="isAuthenticated()"><a th:href="@{/events}">Събития</a></li>
            <li sec:authorize="isAuthenticated()"><a th:href="@{/posts}">Публикации</a></li>
            
            <!-- НОВО: Telegram линк -->
            <li>
                <a href="https://t.me/dragon_dojo_announcements" 
                   target="_blank" 
                   rel="noopener noreferrer"
                   style="color: #0088cc; font-weight: 600;">
                    📱 Telegram
                </a>
            </li>
            
            <li class="home" sec:authorize="isAuthenticated()"><a th:href="@{/home}">Профил</a></li>
            <!-- ...existing menu items... -->
        </ul>
    </div>
</nav>
```

---

## 12. Често задавани въпроси (FAQ)

### Q1: Каква е разликата между Канал и Група?

**Telegram Канал:**
- ✅ Само админи могат да публикуват
- ✅ Потребителите са "subscribers" (абонати)
- ✅ Едностранна комуникация (broadcast)
- ✅ Показва брой абонати
- ✅ По-професионално за обяви

**Telegram Група:**
- ✅ Всички членове могат да пишат (ако не е ограничено)
- ✅ Двустранна комуникация
- ✅ Дискусии и коментари
- ❌ Може да има spam
- ✅ По-интерактивно

**Моята препоръка:** Използвай **Канал** за Dragon Dojo обяви.

---

### Q2: Трябва ли потребителите да имат Telegram акаунт?

**Да**, за да получават нотификации, потребителите трябва да:
1. Имат Telegram акаунт (безплатен)
2. Да се присъединят към канала/групата

**Алтернатива:** Ако някой няма Telegram, може да види публикациите в web апликацията.

---

### Q3: Мога ли да изпращам нотификации само на определени потребители?

**С този подход - НЕ**. Всички членове на канала/групата получават всички нотификации.

**Ако искаш филтриране:**
- Използвай subscription bot подхода (виж `telegram-bot-implementation.md`)
- Или създай **няколко канала** (например: "Треньори", "Състезатели", "Всички")

---

### Q4: Колко струва Telegram Bot API?

**Напълно безплатно!** 🎉

Telegram Bot API е 100% безплатен, няма лимити за брой съобщения или потребители.

**Единствено ограничение:**
- **30 съобщения/секунда** към различни чатове
- За 1 канал → 1 съобщение → няма проблем

---

### Q5: Какво се случва ако Telegram е недостъпен?

Апликацията ще продължи да работи нормално:
- ✅ Постът ще се създаде в базата данни
- ❌ Нотификацията няма да се изпрати
- ✅ Грешката ще се логне
- ✅ Следващия път (когато Telegram е достъпен) ще работи

Това се нарича **graceful degradation** - системата не спира при грешка в една компонента.

---

### Q6: Мога ли да изпращам снимки/видеа?

**Да!** Използвай метода `sendPhotoWithCaption()`:

```java
// Пример: Изпращане на Event banner
String eventBannerUrl = "https://dragondojo.com/images/event-banner.jpg";
String caption = "📅 *Ново събитие:* Състезание в София\n\n⏰ 15.03.2026 10:00";

telegramNotificationService.sendPhotoWithCaption(eventBannerUrl, caption);
```

За видео:

```java
SendVideo video = new SendVideo();
video.setChatId(channelChatId);
video.setVideo(new InputFile("https://example.com/video.mp4"));
video.setCaption("🎥 Видео от тренировката");
execute(video);
```

---

### Q7: Мога ли да изтрия грешно изпратено съобщение?

**Да**, ако ботът е администратор с права за изтриване:

```java
DeleteMessage deleteMessage = new DeleteMessage();
deleteMessage.setChatId(channelChatId);
deleteMessage.setMessageId(messageId); // ID на съобщението
execute(deleteMessage);
```

**Забележка:** Трябва да пазиш `messageId` след изпращане:

```java
Message sentMessage = execute(sendMessage);
int messageId = sentMessage.getMessageId();
// Запази го ако ти трябва
```

---

### Q8: Мога ли да schedule-вам съобщения?

**Telegram API не поддържа native scheduling**, но можеш да използваш Spring `@Scheduled`:

```java
@Service
public class ScheduledNotifications {
    
    @Autowired
    private TelegramNotificationService telegramService;
    
    // Всеки понеделник в 9:00 сутринта
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyReminder() {
        String message = "🥋 *Добро утро, каратисти!*\n\n" +
                       "Нова седмица започва! Не забравяйте тренировките тази седмица:\n" +
                       "- Понеделник 18:00\n" +
                       "- Сряда 18:00\n" +
                       "- Петък 18:00";
        
        telegramService.sendCustomAnnouncement("Седмично напомняне", message);
    }
}
```

---

### Q9: Как мога да видя статистика за канала?

**Ако е Public канал:**
- Telegram показва брой абонати

**За детайлна статистика:**
- Отвори канала → Settings → Statistics (само за канали с >500 абоната)

**Програмно:**
- Telegram Bot API не дава достъп до списък с членове на канала/група (privacy)
- Можеш да проследяваш само брой изпратени съобщения от бота

---

### Q10: Мога ли да интегрирам с Discord, Slack, и т.н.?

**Да!** Архитектурата е същата:

```java
@Service
public class NotificationService {
    
    @Autowired
    private TelegramNotificationService telegramService;
    
    @Autowired
    private DiscordNotificationService discordService; // Подобна имплементация
    
    @Autowired
    private SlackNotificationService slackService;
    
    public void notifyNewPost(Post post) {
        telegramService.notifyNewPost(post);
        discordService.notifyNewPost(post); // Webhook към Discord
        slackService.notifyNewPost(post);   // Webhook към Slack
    }
}
```

---

## 13. Troubleshooting

### Проблем 1: "Bot token is invalid"

**Симптоми:**
```
ERROR: TelegramApiRequestException: 401 Unauthorized
ERROR: Bot token is invalid
```

**Причина:** Невалиден или изтекъл токен.

**Решение:**
1. Отвори @BotFather в Telegram
2. Изпрати `/mybots`
3. Избери своя бот → `API Token`
4. Копирай новия токен
5. Обнови `telegram.bot.token` в `application.properties`
6. Рестартирай апликацията

---

### Проблем 2: "Chat not found"

**Симптоми:**
```
ERROR: TelegramApiRequestException: 400 Bad Request
ERROR: Chat not found
```

**Причина:** Невалиден или грешен Chat ID.

**Решение:**
1. Провери дали Chat ID започва с минус `-` (за канали/групи)
2. Изпрати съобщение в канала/групата
3. Вземи Chat ID отново (виж [Стъпка 5](#5-вземане-на-chat-id))
4. Обнови `telegram.channel.chat.id`
5. Рестартирай апликацията

---

### Проблем 3: "Bot was kicked from the channel"

**Симптоми:**
```
ERROR: TelegramApiRequestException: 403 Forbidden
ERROR: Bot was kicked from the channel
```

**Причина:** Ботът е премахнат от канала/групата или няма права.

**Решение:**
1. Отвори канала/групата в Telegram
2. Settings → Administrators
3. Добави бота отново
4. Дай му права за **Post Messages**
5. Тествай отново

---

### Проблем 4: "Can't parse entities"

**Симптоми:**
```
ERROR: TelegramApiRequestException: 400 Bad Request
ERROR: Can't parse entities: Can't find end of Bold entity
```

**Причина:** Невалиден Markdown синтаксис (липсваща escape на специални символи).

**Решение:**
- Методът `escapeMarkdown()` вече обработва това
- Ако имаш custom съобщения, използвай `escapeMarkdown()`:
  ```java
  String safeText = escapeMarkdown(userInput);
  ```

---

### Проблем 5: "Connection timeout"

**Симптоми:**
```
ERROR: java.net.ConnectException: Connection timed out
```

**Причина:** Няма интернет връзка или firewall блокира Telegram API.

**Решение:**
1. Провери интернет връзката
2. Провери firewall настройки (трябва да има достъп до `api.telegram.org`)
3. Ако си зад корпоративен proxy, конфигурирай proxy:
   ```java
   DefaultBotOptions botOptions = new DefaultBotOptions();
   botOptions.setProxyHost("proxy.example.com");
   botOptions.setProxyPort(8080);
   botOptions.setProxyType(DefaultBotOptions.ProxyType.HTTP);
   ```

---

### Проблем 6: Нотификациите не се изпращат, но няма грешка

**Симптоми:**
- Логовете казват "✅ Изпратено"
- Но няма съобщение в канала

**Причина:** `telegram.notifications.enabled=false` или грешен Chat ID.

**Решение:**
1. Провери `application.properties`:
   ```properties
   telegram.notifications.enabled=true
   ```
2. Провери дали Chat ID е правилен
3. Използвай `/telegram/test` endpoint за директен тест

---

### Проблем 7: Bean creation failed

**Симптоми:**
```
ERROR: Error creating bean with name 'telegramNotificationService'
ERROR: Could not resolve placeholder 'telegram.bot.token'
```

**Причина:** Липсва конфигурация в `application.properties`.

**Решение:**
1. Добави `telegram.bot.token` в properties
2. Добави `telegram.channel.chat.id` в properties
3. Рестартирай апликацията

---

## 14. Deployment инструкции

### Стъпка 1: Build на проекта

```bash
# Windows PowerShell
.\mvnw clean package -DskipTests

# Linux/Mac
./mvnw clean package -DskipTests
```

### Стъпка 2: Задаване на environment variables

**Production сървър (Linux):**

```bash
export TELEGRAM_BOT_TOKEN="123456789:ABCdefGHIjklMNOpqrsTUVwxyz"
export TELEGRAM_CHANNEL_CHAT_ID="-1001234567890"
export APP_BASE_URL="https://dragondojo.com"
```

**Systemd service file:**

```ini
[Unit]
Description=Dragon Dojo Card Index
After=network.target

[Service]
Type=simple
User=dojoadmin
Environment="TELEGRAM_BOT_TOKEN=123456789:ABCdefGHIjklMNOpqrsTUVwxyz"
Environment="TELEGRAM_CHANNEL_CHAT_ID=-1001234567890"
Environment="APP_BASE_URL=https://dragondojo.com"
ExecStart=/usr/bin/java -jar /opt/dojo/DojoCardIndex.jar --spring.profiles.active=prod
Restart=always

[Install]
WantedBy=multi-user.target
```

### Стъпка 3: Стартиране

```bash
java -jar target/DojoCardIndex-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Стъпка 4: Проверка

```bash
# Провери логовете
tail -f logs/spring.log | grep -i telegram

# Трябва да видиш:
# INFO: TelegramNotificationService initialized
```

---

## 15. Поддръжка и Мониторинг

### Health Check Endpoint (Опционално)

```java
@RestController
public class TelegramHealthController {

    @Autowired
    private TelegramNotificationService telegramService;

    @GetMapping("/actuator/telegram/health")
    public ResponseEntity<Map<String, Object>> telegramHealth() {
        Map<String, Object> health = new HashMap<>();
        
        boolean available = telegramService.isAvailable();
        health.put("status", available ? "UP" : "DOWN");
        health.put("service", "Telegram Notifications");
        health.put("enabled", available);
        
        return ResponseEntity.ok(health);
    }
}
```

**Използване:**
```bash
curl http://localhost:8080/actuator/telegram/health
```

**Отговор:**
```json
{
  "status": "UP",
  "service": "Telegram Notifications",
  "enabled": true
}
```

---

## 🎉 Заключение

### Какво постигнахме:

✅ **Супер проста имплементация** - 1 клас, ~150 реда код  
✅ **Нулеви база данни промени** - не пипаме User модела  
✅ **Broadcast нотификации** - 1 съобщение → всички го виждат  
✅ **Лесен onboarding** - потребителите просто се join-ват  
✅ **Минимални ресурси** - няма subscriptions, кеширане, и т.н.  
✅ **Production-ready** - error handling, logging, graceful degradation  
✅ **Разширяем** - лесно добавяне на нотификации за събития, и т.н.  

---

## 📊 Сравнение: Преди vs След

### ПРЕДИ:
- ❌ Няма нотификации извън апликацията
- ❌ Потребителите трябва да влизат редовно за да виждат новини
- ❌ Няма push нотификации

### СЛЕД:
- ✅ Моментални Telegram нотификации
- ✅ Потребителите виждат обяви директно на телефона
- ✅ Автоматично при нови публикации/събития
- ✅ 100% безплатно и лесно

---

## 📚 Следващи стъпки

1. **Прочети документацията** внимателно
2. **Тествай локално** преди production deploy
3. **Информирай потребителите** за новия Telegram канал
4. **Мониторинг** - следи логовете първите дни
5. **Feedback** - питай потребителите дали нотификациите работят

---

## 🔗 Полезни ресурси

- **Telegram Bot API документация:** https://core.telegram.org/bots/api
- **Java Telegram Bots Library:** https://github.com/rubenlagus/TelegramBots
- **Markdown синтаксис в Telegram:** https://core.telegram.org/bots/api#markdown-style

---

**Автор:** GitHub Copilot  
**Дата:** 27 февруари 2026  
**Версия:** 1.0  
**Име на проект:** Dragon Dojo Card Index  

---

**Успех с имплементацията! Ако имаш въпроси, питай! 🚀🥋**

