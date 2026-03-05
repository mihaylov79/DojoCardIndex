# 📢 Telegram Канал/Група Нотификации - Модерна Имплементация (Актуализирана)

**Дата:** 1 март 2026  
**Цел:** Изпращане на нотификации за нови публикации и събития в Telegram канал/група  
**Сложност:** ⭐ Лесна (1-2 часа)  
**Актуализация:** Използва модерен подход с `RestTemplate` вместо deprecated `DefaultAbsSender`

---

## 🆕 Какво е променено спрямо оригиналния гайд?

### ❌ Стар подход (deprecated):
```java
public class TelegramNotificationService extends DefaultAbsSender {
    // DefaultAbsSender е deprecated в по-нови версии
}
```

### ✅ Нов подход (препоръчителен):
```java
public class TelegramNotificationService {
    // Директни HTTP заявки към Telegram API чрез RestTemplate
    // По-прост, по-лесен за разбиране, без deprecated код
}
```

---

## 📋 Съдържание

1. [Защо Telegram Канал вместо Bot Subscriptions](#1-защо-telegram-канал-вместо-bot-subscriptions)
2. [Как работи системата](#2-как-работи-системата)
3. [Подготовка - Създаване на Telegram Bot](#3-подготовка---създаване-на-telegram-bot)
4. [Създаване на Telegram Канал или Група](#4-създаване-на-telegram-канал-или-група)
5. [Вземане на Chat ID](#5-вземане-на-chat-id)
6. [Промени в зависимостите (pom.xml)](#6-промени-в-зависимостите-pomxml)
7. [Java имплементация (АКТУАЛИЗИРАНО)](#7-java-имплементация-актуализирано)
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
| **Код (брой редове)** | ~800+ | ~100 |
| **Java класове** | 5+ нови | **1 нов** |
| **База данни промени** | 2 таблици + миграции | **0 промени** |
| **Външни библиотеки** | Telegram Bots SDK | **Само Spring Web (вече имаш)** |
| **Генериране на кодове** | Да (сложна логика) | **Не е нужно** |
| **Асинхронни проверки** | Да (при всеки логин) | **Не е нужно** |
| **Кеширане** | Да (Redis/In-Memory) | **Не е нужно** |
| **Конфигурация** | Много properties | **3 реда** |
| **Поддръжка** | Средна сложност | **Минимална** |
| **Onboarding на потребител** | Копиране на код + /subscribe | **1 клик (Join)** |
| **Ресурси (CPU/Memory)** | Средни | **Минимални** |
| **Telegram API заявки** | N (брой потребители) | **1 заявка** |
| **Rate limits риск** | Да (при >30 потребители/сек) | **Не** |
| **Време за имплементация** | 4-6 часа | **1-2 часа** |
| **Deprecated код** | Да (`DefaultAbsSender`) | **Не!** ✅ |

### ✅ Предимства на новия подход:

1. **Супер проста имплементация** - 1 клас, ~100 реда код
2. **Няма deprecated API** - използваме директни HTTP заявки
3. **По-лесно за разбиране** - прозрачен HTTP POST към Telegram API
4. **Нулеви база данни промени** - не пипаме User модела
5. **Broadcast нотификации** - перфектно за обяви
6. **Работи веднага** - без сложна логика
7. **По-малко dependency conflicts** - не разчитаме на специализирани библиотеки

### ⚠️ Недостатъци:

1. ❌ Няма персонализация (всички виждат всичко)
2. ❌ Не можеш да филтрираш по роля
3. ❌ Няма статистика кой е видял какво
4. ❌ Всички или никой (не можеш да изпратиш само на треньори)

---

## 2. Как работи системата

### 📊 Технически поток:

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
│  СТЪПКА 2: ИЗПРАЩАНЕ НА НОТИФИКАЦИЯ (Автоматично)                     │
│                                                                         │
│  Dragon Dojo App                    Telegram API                       │
│  ─────────────────                  ────────────                       │
│                                                                         │
│  PostService.createNewPost()                                           │
│       │                                                                 │
│       ├─> Записва пост в DB ✅                                         │
│       │                                                                 │
│       └─> TelegramNotificationService.notifyNewPost(post)              │
│               │                                                         │
│               ├─> buildPostMessage(post) → Markdown текст              │
│               │                                                         │
│               └─> RestTemplate.postForObject()                         │
│                       │                                                 │
│                       │   HTTP POST                                     │
│                       ├──────────────────────────────────>              │
│                       │   URL: https://api.telegram.org/bot{TOKEN}/... │
│                       │   Body: { chat_id: "-100...", text: "..." }    │
│                       │                                                 │
│                       │   HTTP 200 OK                                   │
│                       <──────────────────────────────────               │
│                       │   { ok: true, result: {...} }                   │
│                       │                                                 │
│                       └─> Логва успех ✅                               │
│                                                                         │
│                                         └─> Изпраща към всички членове │
│                                             на канала! 📢               │
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
│  📅 01.03.2026 14:30                                                    │
│                                                                         │
│  ➡️ Прочети повече                                                     │
│  [Link към пълната публикация]                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 🔧 Технически детайли на новия подход:

**Telegram Bot API endpoint:**
```
POST https://api.telegram.org/bot{BOT_TOKEN}/sendMessage
```

**Request Body:**
```json
{
  "chat_id": "-1001234567890",
  "text": "📢 *Нова публикация в Dragon Dojo*\n\n📝 *Заглавие*\n...",
  "parse_mode": "Markdown",
  "disable_web_page_preview": true
}
```

**Response:**
```json
{
  "ok": true,
  "result": {
    "message_id": 123,
    "chat": {
      "id": -1001234567890,
      "title": "Dragon Dojo Обяви",
      "type": "channel"
    },
    "text": "..."
  }
}
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

### Стъпка 2: Персонализиране на бота (опционално)

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

### Метод 1: Чрез Telegram API (Най-лесен)

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

### Метод 2: Чрез специализиран бот

1. Намери бота **@getidsbot** в Telegram
2. Препрати му съобщение от канала/групата
3. Ботът ще ти отговори с Chat ID

или

1. Добави бота **@raw_data_bot** в групата/канала
2. Изпрати съобщение
3. Ботът ще отговори с JSON, включително Chat ID

---

## 6. Промени в зависимостите (pom.xml)

### ⚠️ ВАЖНО: Не са нужни нови dependencies!

**Добрата новина:** За новия подход **НЕ** трябва да добавяш Telegram Bots библиотеката!

Използваме само `RestTemplate` от Spring, който **вече е част от проекта**.

**Ако имаш добавена старата dependency, можеш да я премахнеш (опционално):**

```xml
<!-- СТАРА dependency (може да се премахне, но не е задължително) -->
<dependency>
    <groupId>org.telegram</groupId>
    <artifactId>telegrambots</artifactId>
    <version>6.9.7.1</version>
</dependency>
```

**Новият подход използва само:**
- `spring-boot-starter-web` (вече имаш)
- `RestTemplate` (built-in в Spring)

---

## 7. Java имплементация (АКТУАЛИЗИРАНО)

### 7.1. 📁 `TelegramNotificationService.java` (МОДЕРЕН ПОДХОД)

**Локация:** `src/main/java/cardindex/dojocardindex/telegram/TelegramNotificationService.java`

**Създай новата директория:**
```
src/main/java/cardindex/dojocardindex/
└── telegram/
    └── TelegramNotificationService.java
```

**Пълен код (НОВА ВЕРСИЯ):**

```java
package cardindex.dojocardindex.telegram;

import cardindex.dojocardindex.Event.models.Event;
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

/**
 * Service за изпращане на нотификации в Telegram канал/група.
 * 
 * МОДЕРЕН ПОДХОД (без deprecated код):
 * - Използва директни HTTP заявки към Telegram Bot API
 * - Няма зависимост от telegrambots библиотеката
 * - По-прост и лесен за разбиране
 * - 1 Chat ID → Всички членове на канала/групата получават нотификация
 * - Няма subscriptions, кодове, база данни промени
 * 
 * @author Dragon Dojo Team
 * @version 2.0 (Updated - без deprecated API)
 */
@Service
@Slf4j
public class TelegramNotificationService {

    /**
     * Telegram Bot Token (от BotFather)
     */
    private final String botToken;

    /**
     * Chat ID на канала/групата (например: -1001234567890)
     */
    private final String channelChatId;

    /**
     * Base URL на апликацията (за линкове в съобщенията)
     */
    private final String appBaseUrl;

    /**
     * Дали нотификациите са включени (може да се изключат временно)
     */
    private final boolean notificationsEnabled;

    /**
     * RestTemplate за HTTP заявки към Telegram API
     */
    private final RestTemplate restTemplate;

    /**
     * Base URL на Telegram Bot API
     */
    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    /**
     * Constructor с dependency injection.
     * 
     * @param botToken Telegram bot token от application.properties
     * @param channelChatId Chat ID на канала/групата
     * @param appBaseUrl Base URL на апликацията
     * @param notificationsEnabled Дали нотификациите са включени
     */
    public TelegramNotificationService(
            @Value("${telegram.bot.token}") String botToken,
            @Value("${telegram.channel.chat.id}") String channelChatId,
            @Value("${app.base.url:http://localhost:8080}") String appBaseUrl,
            @Value("${telegram.notifications.enabled:true}") boolean notificationsEnabled) {
        this.botToken = botToken;
        this.channelChatId = channelChatId;
        this.appBaseUrl = appBaseUrl;
        this.notificationsEnabled = notificationsEnabled;
        this.restTemplate = new RestTemplate();
        
        log.info("🤖 Telegram Notification Service инициализиран");
        log.info("   - Нотификациите са: {}", notificationsEnabled ? "ВКЛЮЧЕНИ ✅" : "ИЗКЛЮЧЕНИ ❌");
        log.info("   - Chat ID: {}", channelChatId);
    }

    /**
     * Изпращане на нотификация за нов пост в канала/групата.
     * 
     * @param post Новата публикация
     */
    public void notifyNewPost(Post post) {
        if (!notificationsEnabled) {
            log.debug("Telegram нотификациите са изключени (telegram.notifications.enabled=false)");
            return;
        }

        String message = buildPostMessage(post);
        sendMessage(message);
        
        log.info("📢 Telegram нотификация изпратена за пост: {}", post.getTitle());
    }

    /**
     * Изпращане на нотификация за ново събитие.
     * 
     * @param event Новото събитие
     */
    public void notifyNewEvent(Event event) {
        if (!notificationsEnabled) {
            log.debug("Telegram нотификациите са изключени");
            return;
        }

        String message = buildEventMessage(event);
        sendMessage(message);
        
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
            log.debug("Telegram нотификациите са изключени");
            return;
        }

        String message = String.format(
            "📣 *%s*\n\n%s",
            escapeMarkdown(title),
            escapeMarkdown(content)
        );
        
        sendMessage(message);
        log.info("📣 Custom обява изпратена: {}", title);
    }

    /**
     * Основен метод за изпращане на текстово съобщение в канала/групата.
     * 
     * Изпраща HTTP POST заявка към Telegram Bot API:
     * POST https://api.telegram.org/bot{TOKEN}/sendMessage
     * 
     * @param text Текст на съобщението (поддържа Markdown)
     */
    private void sendMessage(String text) {
        try {
            String url = TELEGRAM_API_URL + botToken + "/sendMessage";
            
            // Изграждане на request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", channelChatId);
            requestBody.put("text", text);
            requestBody.put("parse_mode", "Markdown");
            requestBody.put("disable_web_page_preview", true);
            
            // Задаване на headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // Изпращане на HTTP заявка
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("✅ Telegram нотификация изпратена успешно");
            } else {
                log.warn("⚠️ Telegram API върна статус: {}", response.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("❌ Грешка при изпращане на Telegram нотификация: {}", e.getMessage());
            log.error("Проверете:");
            log.error("  - telegram.bot.token в application.properties");
            log.error("  - telegram.channel.chat.id в application.properties");
            log.error("  - Дали ботът е администратор в канала/групата");
            log.error("  - Дали ботът има права за изпращане на съобщения");
            log.error("  - Дали имате интернет връзка");
            log.debug("Stack trace:", e);
        }
    }

    /**
     * Изграждане на съобщение за нов пост.
     * 
     * @param post Пост за който се изгражда съобщението
     * @return Форматирано съобщение с Markdown
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
     * 
     * @param event Събитие за което се изгражда съобщението
     * @return Форматирано съобщение с Markdown
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
     * Escape на специални символи за Telegram Markdown.
     * 
     * Telegram Markdown изисква escape на специални символи,
     * иначе хвърля грешка при изпращане.
     * 
     * @param text Текст за escape
     * @return Escaped текст
     */
    private String escapeMarkdown(String text) {
        if (text == null) {
            return "";
        }
        
        // Escape на основните специални символи за Markdown
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
                           "Това е тестово съобщение от Dragon Dojo системата\\.\n" +
                           "Ако виждаш това съобщение, всичко е настроено правилно\\! 🥋";
        sendMessage(testMessage);
    }

    /**
     * Изпращане на снимка със съобщение (например Event banner).
     * 
     * @param photoUrl URL на снимката
     * @param caption Текст под снимката
     */
    public void sendPhoto(String photoUrl, String caption) {
        if (!notificationsEnabled) {
            log.debug("Telegram нотификациите са изключени");
            return;
        }

        try {
            String url = TELEGRAM_API_URL + botToken + "/sendPhoto";
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("chat_id", channelChatId);
            requestBody.put("photo", photoUrl);
            requestBody.put("caption", caption);
            requestBody.put("parse_mode", "Markdown");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("📸 Снимка изпратена в Telegram канала");
            }
            
        } catch (Exception e) {
            log.error("❌ Грешка при изпращане на снимка в Telegram: {}", e.getMessage());
        }
    }
}
```

---

### 🔍 Какво е различно в новата версия?

| Аспект | Стар подход | Нов подход |
|--------|-------------|------------|
| **Наследяване** | `extends DefaultAbsSender` | Обикновен `@Service` клас |
| **Dependencies** | `telegrambots` библиотека | Само `RestTemplate` |
| **API извиквания** | `execute(sendMessage)` | `restTemplate.postForEntity()` |
| **Deprecated код** | ❌ Да | ✅ Не |
| **Лесно за разбиране** | ⚠️ Средно (скрива HTTP логиката) | ✅ Много (прозрачни HTTP заявки) |
| **Brой редове** | ~180 | ~100 |
| **Debugging** | По-труден | По-лесен (виждаш HTTP заявките) |

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

    private final EventRepository eventRepository;
    private final UserService userService;
    private final TelegramNotificationService telegramNotificationService; // НОВО!

    @Autowired
    public EventService(EventRepository eventRepository,
                        UserService userService,
                        TelegramNotificationService telegramNotificationService) { // НОВО!
        this.eventRepository = eventRepository;
        this.userService = userService;
        this.telegramNotificationService = telegramNotificationService; // НОВО!
    }

    // ...existing methods...

    public void addNewEvent(CreateEventRequest createEventRequest) {
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

**За проекта ти (вече имаш тези редове):**
```properties
#Telegram Notifications Configuration
telegram.bot.token=8741909830:AAGB6EubfiBcvQdD2tTW80hBegw5UUkGeRc
telegram.channel.chat.id=-1003780816457
telegram.notifications.enabled=true
app.base.url=http://localhost:8080
```

✅ Конфигурацията ти е **вече правилна**!

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

**Задаване на environment variables в Azure/Production:**

```bash
# Azure CLI
az webapp config appsettings set --name dragon-dojo-app \
  --resource-group dragon-dojo-rg \
  --settings \
    TELEGRAM_BOT_TOKEN="8741909830:AAGB6EubfiBcvQdD2tTW80hBegw5UUkGeRc" \
    TELEGRAM_CHANNEL_CHAT_ID="-1003780816457" \
    APP_BASE_URL="https://dragondojo.com"
```

---

## 10. Тестване

### 10.1. Тест 1: Проверка на конфигурацията

1. **Стартирай апликацията:**
   ```powershell
   .\mvnw spring-boot:run
   ```

2. **Провери логовете:**
   ```
   INFO: 🤖 Telegram Notification Service инициализиран
   INFO:    - Нотификациите са: ВКЛЮЧЕНИ ✅
   INFO:    - Chat ID: -1003780816457
   ```

---

### 10.2. Тест 2: Изпращане на тестово съобщение

**Създай временен test controller:**

```java
package cardindex.dojocardindex.telegram;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TelegramTestController {

    @Autowired
    private TelegramNotificationService telegramService;

    /**
     * ВРЕМЕНЕН endpoint за тестване.
     * Отвори: http://localhost:8080/telegram/test
     * ИЗТРИЙ след тестване!
     */
    @GetMapping("/telegram/test")
    public String sendTest() {
        try {
            telegramService.sendTestMessage();
            return "✅ Тестово съобщение изпратено! Провери канала в Telegram.";
        } catch (Exception e) {
            return "❌ Грешка: " + e.getMessage();
        }
    }

    @GetMapping("/telegram/health")
    public String checkHealth() {
        boolean available = telegramService.isAvailable();
        return available 
            ? "✅ Telegram service е активен и конфигуриран правилно" 
            : "❌ Telegram service не е достъпен или не е конфигуриран";
    }
}
```

**Използване:**
1. Стартирай апликацията
2. Отвори браузър: `http://localhost:8080/telegram/test`
3. Провери Telegram канала - трябва да видиш: "✅ Telegram нотификациите работят!"
4. **ИЗТРИЙ `TelegramTestController` след теста!**

---

### 10.3. Тест 3: Реален тест с нова публикация

1. **Влез в апликацията** като админ
2. **Създай нова публикация**
3. **Провери логовете:**
   ```
   INFO: 📢 Telegram нотификация изпратена за пост: [Заглавие]
   DEBUG: ✅ Telegram нотификация изпратена успешно
   ```
4. **Отвори Telegram** → Провери канала
5. **Трябва да видиш съобщение!** 🎉

---

### 10.4. Тест 4: Graceful degradation (изключен интернет)

1. **Изключи интернета** временно
2. **Създай нова публикация**
3. **Провери логовете:**
   ```
   ERROR: ❌ Грешка при изпращане на Telegram нотификация
   INFO: Post created successfully
   ```
4. **Потвърди:** Постът е създаден в базата данни ✅
5. **Включи интернета**
6. **Създай друга публикация** → сега нотификацията трябва да работи ✅

---

## 11. Frontend промени (опционално)

### 11.1. Добавяне на бутон в профилната страница

Можеш да добавиш бутон в `user-details.html`, който води към Telegram канала.

⚠️ **ВАЖНО: Public vs Private канал**

#### Вариант А: Public канал (препоръчително) ✅

**Как да направиш канала публичен:**
1. Отвори канала в Telegram
2. Settings → Channel Type → **Public**
3. Задай username (например: `dragon_dojo_bg`)
4. Линкът ще бъде: `https://t.me/dragon_dojo_bg`

**HTML код за публичен канал:**

```html
<!-- Telegram Канал секция -->
<div class="telegram-section" style="margin-top: 2rem; padding: 1.5rem; background: #f8f9fa; border-radius: 8px;">
    <h3>📱 Telegram Обяви</h3>
    <p>Получавай моментални нотификации за нови публикации и събития!</p>
    <a href="https://t.me/dragon_dojo_bg" 
       target="_blank" 
       class="btn btn-telegram"
       style="background: #0088cc; color: white; padding: 0.75rem 1.5rem; border-radius: 6px; text-decoration: none;">
        🔔 Присъедини се към канала
    </a>
</div>
```

#### Вариант Б: Private канал (с invite link) ⚠️

Ако искаш каналът да остане private:

1. Отвори канала в Telegram
2. Settings → Invite Links → **Create a new link**
3. Копирай постоянния линк (например: `https://t.me/+ABC123xyz...`)

**HTML код за частен канал:**

```html
<!-- Telegram Канал секция -->
<div class="telegram-section" style="margin-top: 2rem; padding: 1.5rem; background: #f8f9fa; border-radius: 8px;">
    <h3>📱 Telegram Обяви</h3>
    <p>Получавай моментални нотификации за нови публикации и събития!</p>
    <a href="https://t.me/+ABC123xyz..." 
       target="_blank" 
       class="btn btn-telegram"
       style="background: #0088cc; color: white; padding: 0.75rem 1.5rem; border-radius: 6px; text-decoration: none;">
        🔔 Присъедини се към канала
    </a>
    <p style="margin-top: 0.5rem; font-size: 0.9rem; color: #666;">
        ⚠️ Invite линкът е само за членове на доджото
    </p>
</div>
```

**⚠️ Недостатъци на private канал:**
- Invite линкът може да бъде споделен с външни хора
- Ако промениш линка, старият спира да работи
- По-трудна поддръжка

**✅ Препоръка:** Използвай **public канал** за по-лесно управление!

**Забележка:** Замени линка с реалния линк на твоя канал!

---

## 12. Често задавани въпроси (FAQ)

### Q1: Защо новият подход е по-добър от стария?

**Отговор:**

| Критерий | Стар подход (`DefaultAbsSender`) | Нов подход (`RestTemplate`) |
|----------|----------------------------------|------------------------------|
| **Deprecated** | ❌ Да | ✅ Не |
| **Dependency conflicts** | ⚠️ Може да има | ✅ Няма |
| **Лесно за разбиране** | ⚠️ Средно | ✅ Много |
| **Debugging** | ⚠️ По-труден | ✅ Лесен |
| **HTTP прозрачност** | ❌ Скрит | ✅ Прозрачен |
| **Build size** | ⚠️ +2MB (telegrambots lib) | ✅ 0 MB (built-in) |

---

### Q2: Мога ли да използвам WebClient вместо RestTemplate?

**Отговор:** Да! `WebClient` е модерната алтернатива за reactive приложения:

```java
@Service
public class TelegramNotificationService {
    
    private final WebClient webClient;
    
    public TelegramNotificationService(...) {
        this.webClient = WebClient.builder()
            .baseUrl(TELEGRAM_API_URL + botToken)
            .build();
    }
    
    private void sendMessage(String text) {
        webClient.post()
            .uri("/sendMessage")
            .bodyValue(Map.of(
                "chat_id", channelChatId,
                "text", text,
                "parse_mode", "Markdown"
            ))
            .retrieve()
            .bodyToMono(String.class)
            .subscribe(
                response -> log.debug("✅ Изпратено"),
                error -> log.error("❌ Грешка: {}", error.getMessage())
            );
    }
}
```

---

### Q3: Колко струва Telegram Bot API?

**Отговор:** **Напълно безплатно!** 🎉

- Няма лимити за брой съобщения
- Няма лимити за брой потребители
- Единствено ограничение: **30 съобщения/секунда**

---

### Q4: Какво се случва ако Telegram е недостъпен?

**Отговор:** Апликацията продължава да работи нормално:

- ✅ Постът се създава в базата данни
- ❌ Нотификацията не се изпраща
- ✅ Грешката се логва
- ✅ Следващия път ще работи

Това се нарича **graceful degradation**.

---

### Q5: Мога ли да изпращам снимки/видеа/документи?

**Отговор:** Да! Използвай съответните endpoints:

```java
// Снимка
POST https://api.telegram.org/bot{TOKEN}/sendPhoto
Body: { "chat_id": "...", "photo": "URL или file_id" }

// Видео
POST https://api.telegram.org/bot{TOKEN}/sendVideo
Body: { "chat_id": "...", "video": "URL или file_id" }

// Документ
POST https://api.telegram.org/bot{TOKEN}/sendDocument
Body: { "chat_id": "...", "document": "URL или file_id" }
```

---

## 13. Troubleshooting

### Проблем 1: "Cannot resolve configuration property 'telegram.bot.token'"

**Причина:** IntelliJ не разпознава новите properties.

**Решение:**
1. Рестартирай IntelliJ
2. Или добави в `spring-configuration-metadata.json` (опционално)
3. Предупреждението не пречи на работата

---

### Проблем 2: "401 Unauthorized"

**Причина:** Невалиден bot token.

**Решение:**
1. Провери `telegram.bot.token` в `application.properties`
2. Ако токенът е компрометиран, генерирай нов от @BotFather:
   ```
   /mybots → Избери бот → API Token → Revoke → Generate new token
   ```

---

### Проблем 3: "400 Bad Request: Chat not found"

**Причина:** Невалиден Chat ID или ботът не е в канала.

**Решение:**
1. Провери дали Chat ID започва с минус `-`
2. Провери дали ботът е администратор в канала
3. Вземи Chat ID отново (виж [Стъпка 5](#5-вземане-на-chat-id))

---

### Проблем 4: "403 Forbidden: Bot was kicked"

**Причина:** Ботът е премахнат от канала.

**Решение:**
1. Отвори канала → Administrators
2. Добави бота отново
3. Дай му права за **Post Messages**

---

### Проблем 5: "Connection timeout"

**Причина:** Няма интернет или firewall блокира Telegram.

**Решение:**
1. Провери интернет връзката
2. Провери firewall (трябва достъп до `api.telegram.org`)
3. Тествай с:
   ```powershell
   curl https://api.telegram.org/bot{TOKEN}/getMe
   ```

---

## 🎉 Заключение

### Какво постигнахме:

✅ **Модерен подход** - без deprecated API  
✅ **Супер проста имплементация** - ~100 реда код  
✅ **Нулеви външни dependencies** - само Spring Web  
✅ **Прозрачни HTTP заявки** - лесно за debugging  
✅ **Production-ready** - error handling, logging, graceful degradation  
✅ **По-малък build size** - няма telegrambots библиотека  

---

## 📚 Следващи стъпки

1. **Прочети документацията** внимателно
2. **Създай и конфигурирай бота** в Telegram
3. **Добави кода** в проекта (следвай точка 7)
4. **Тествай локално** преди production deploy
5. **Информирай потребителите** за новия канал

---

## 🔗 Полезни ресурси

- **Telegram Bot API документация:** https://core.telegram.org/bots/api
- **Telegram Bot API sendMessage:** https://core.telegram.org/bots/api#sendmessage
- **Spring RestTemplate:** https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html

---

**Автор:** GitHub Copilot  
**Дата:** 1 март 2026  
**Версия:** 2.0 (Актуализирана - без deprecated API)  
**Проект:** Dragon Dojo Card Index  

---

**Успех с имплементацията! Ако имаш въпроси, питай! 🚀🥋**

