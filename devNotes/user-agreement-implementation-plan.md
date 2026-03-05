# User Agreement Implementation Plan
## Родителско съгласие и споразумения за потребители

**Дата:** 2026-03-05  
**Статус:** ✅ ПОТВЪРДЕНО - Готово за имплементация  
**Цел:** Дигитализация на хартиеното споразумение с родителско съгласие за непълнолетни

**Ключови решения:**
- ✅ Едно споразумение (LIABILITY_WAIVER)
- ✅ Нов `/parent-consent` endpoint в mail-svc
- ✅ User сам приема (без ADMIN consent)
- ✅ Веднага деактивация при отказ
- ✅ Export функционалност (CSV, Excel, PDF, JSON)

---

## 📋 Съдържание
1. [Бизнес изисквания](#бизнес-изисквания)
2. [Правна рамка](#правна-рамка)
3. [Текуща архитектура](#текуща-архитектура)
4. [Анализ на mail-svc](#анализ-на-mail-svc)
5. [Предложена архитектура](#предложена-архитектура)
6. [Промени в DojoCardIndex](#промени-в-dojocardindex)
7. [Промени в mail-svc](#промени-в-mail-svc)
8. [Workflow схеми](#workflow-схеми)
9. [База данни](#база-данни)
10. [Audit Trail](#audit-trail)
11. [Export & Archive функционалност](#export--archive-функционалност)
12. [Имплементационен план](#имплементационен-план)
13. [Решения (Потвърдени)](#решения-потвърдени-от-клиента---2026-03-05)

---

## 🚀 Quick Reference - Ключови решения

### **Архитектура:**
| Компонент | Решение |
|-----------|---------|
| Agreement типове | **Само едно** - LIABILITY_WAIVER (включва всичко) |
| ConsentMethod | **DIRECT** и **PARENT_EMAIL** (без ADMIN) |
| User.agreed поле | **НЕ се добавя** - проследява се само в UserAgreement |
| Deactivation | **Веднага** при отказ (без grace period) |
| Existing users | **Приемат при login** (без bulk import) |

### **Mail-svc интеграция:**
| Опция | Избрана |
|-------|---------|
| Използване на `/forgotten-password` | ❌ НЕ |
| Нов `/parent-consent` endpoint | ✅ ДА |

### **Entities:**
```
Agreement           → Документът (version, content, type)
UserAgreement       → Audit trail (user, agreement, timestamps, IPs)
  └─ agreementPending → Boolean вратичка (временно отложено подписване)
User.contactPersonEmail → НОВ field (nullable=TRUE, задължителен само за деца)
User.birthDate      → nullable=false
```

### **Export формати:**
```
Приоритет 1: CSV + Excel (critical за архива)
Приоритет 2: PDF + JSON (nice-to-have)
```

---

## 🎯 Бизнес изисквания

### **Проблем:**
- Всеки тренуващ сега подписва **хартиено споразумение**
- Много от потребителите са **деца (непълнолетни)**
- Процесът е **бавен** и изисква физическо присъствие
- Необходимост от **дигитализация** и **законосъобразност**

### **Цел:**
1. ✅ Потребителят да вижда споразумението при регистрация
2. ✅ Ако се съгласи → `agreed = true`, `agreementDate = LocalDate.now()`
3. ✅ При всеки login → проверка дали е приел последната версия
4. ✅ Ако е дете (< 18 години) → изпращане на email на `contactPersonEmail` за родителско потвърждение
5. ✅ Ако потребителят откаже → предупреждение и деактивация на акаунт
6. ✅ Проследяване на версии на споразумението
7. ✅ Audit trail - доказателство пред властите

---

## ⚖️ Правна рамка

### **Електронен документ vs. Хартиен:**
- Електронните документи имат **правна сила** (ЗЕДЕУУ)
- Електронното съгласие е валидно при правилна имплементация

### **Изисквания за непълнолетни:**
| Възраст | Изискване | Имплементация |
|---------|-----------|---------------|
| < 14 години | Родителско съгласие ЗАДЪЛЖИТЕЛНО | Email верификация + родителски потвърждение |
| 14-18 години | Препоръчително родителско съгласие | Email верификация + родителски потвърждение |
| 18+ години | Директно съгласие | Checkbox + потвърждение |

### **Необходими елементи за правна валидност:**
1. ✅ **WHO** - Идентификация (име, ЕГН/дата на раждане, email)
2. ✅ **WHEN** - Timestamp (дата и час)
3. ✅ **WHAT** - Версия и съдържание на документа
4. ✅ **WHERE** - IP адрес
5. ✅ **HOW** - Метод на съгласие (директно/parent email/admin)

### **Съхранение:**
- Минимум **5 години** след прекратяване на членството
- Защитена база данни с audit trail
- Възможност за генериране на PDF доказателство

---

## 🏗️ Текуща архитектура

### **User Entity - Текущо състояние:**

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(name = "birth_date")  // ⚠️ nullable = true (трябва да стане false)
    private LocalDate birthDate;
    
    @Column(name = "contact_person")
    private String contactPerson;  // ✅ Име на контактно лице
    
    @Column(name = "contact_person_phone")
    private String contactPersonPhone;  // ✅ Телефон
    
    // ❌ ЛИПСВА contactPersonEmail - трябва да добавим!
    
    // ❌ ЛИПСВА agreed, agreementDate - но НЕ трябва да ги добавяме в User!
    // Вместо това ще ги пазим в отделна entity UserAgreement
}
```

### **Текущ Email механизъм:**

#### **1. Обикновени notifications (регистрирани потребители):**
```java
// NotificationService.sendNotification()
NotificationRequest request = NotificationRequest.builder()
    .recipientId(UUID)      // ✅ Задължително!
    .firstName(String)
    .lastName(String)
    .title(String)
    .content(String)
    .build();

notificationClient.sendEmail(request);
```

**Как работи:**
- Изисква `recipientId` (UUID на user)
- Търси `NotificationPreference` по `recipientId`
- Взима email от `NotificationPreference.info`
- Проверява дали са enabled notifications
- Записва в notification history

**Проблем за parent consent:**
- ❌ Родителят може да НЯМА `recipientId` (не е регистриран user)
- ❌ Не можем да използваме този endpoint

#### **2. Forgotten password (произволен email):**
```java
// ForgottenPasswordTokenService.sendForgottenPasswordEmail()
ForgottenPasswordRequest request = ForgottenPasswordRequest.builder()
    .recipient(String)  // ✅ Директно email адрес!
    .title(String)
    .content(String)    // ✅ HTML съдържание
    .build();

notificationClient.sendForgottenPasswordEmail(request);
```

**Как работи:**
- НЕ изисква `recipientId`
- Приема **произволен email** директно
- НЕ проверява `NotificationPreference`
- НЕ записва в notification history
- Използва template `forgotten-password.html`

**Плюсове за parent consent:**
- ✅ Може да изпратим на `contactPersonEmail`
- ✅ Работи веднага (не изисква промени в mail-svc)
- ✅ Поддържа HTML content

**Минуси:**
- ⚠️ Naming confusion (endpoint се казва `/forgotten-password`)

---

## 📧 Анализ на mail-svc

### **GitHub Repository:**
```
https://github.com/mihaylov79/mail-svc
```

### **Структура:**
```
mail-svc/
├── model/
│   ├── ForgottenPasswordRequest.java
│   ├── Notification.java
│   ├── NotificationPreference.java
│   └── NotificationStatus.java
├── repository/
│   ├── NotificationRepository.java
│   └── NotificationPreferenceRepository.java
├── service/
│   └── NotificationService.java
└── web/
    ├── NotificationController.java
    └── dto/
        ├── NotificationRequest.java
        ├── NotificationPreferenceRequest.java
        └── NotificationResponse.java
```

### **API Endpoints:**

#### **1. Notification Endpoints (изискват recipientId):**
```http
POST   /api/v1/notifications              # Изпращане на notification
GET    /api/v1/notifications              # История на notifications
DELETE /api/v1/notifications              # Изтриване на история
GET    /api/v1/notifications/preferences  # Предпочитания
POST   /api/v1/notifications/preferences  # Update предпочитания
PUT    /api/v1/notifications/preferences  # Промяна на enabled status
```

#### **2. Forgotten Password Endpoint (произволен email):**
```http
POST /api/v1/notifications/forgotten-password
```

**Request body:**
```json
{
  "recipient": "parent@example.com",
  "title": "Email заглавие",
  "content": "<p>HTML съдържание...</p>"
}
```

### **NotificationService.sendForgottenPasswordLink() - Код анализ:**

```java
public void sendForgottenPasswordLink(ForgottenPasswordRequest request){
    Context context = new Context();
    context.setVariable("content", request.getContent());
    
    String htmlContent = templateEngine.process("forgotten-password", context);
    
    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(request.getRecipient());      // ✅ Директно на email-а!
        helper.setSubject(request.getTitle());
        helper.setText(htmlContent, true);         // ✅ HTML content
        mailSender.send(message);
    } catch (MessagingException e) {
        log.warn("Изпращането на връзка до {} беше неуспешно!", 
                 request.getRecipient(), e);
        throw new RuntimeException("Мейл не беше изпратен!");
    }
}
```

**Ключови моменти:**
- ✅ Приема `recipient` като String (email)
- ✅ НЕ проверява `NotificationPreference`
- ✅ НЕ записва в `Notification` table
- ✅ Използва Thymeleaf template `forgotten-password.html`
- ✅ HTML съдържание се подава в `content` променливата

### **Template: forgotten-password.html**
```html
<!-- В mail-svc/src/main/resources/templates/forgotten-password.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
    <div th:utext="${content}"></div>  <!-- ✅ Рендерира HTML content -->
</body>
</html>
```

---

## 🎨 Предложена архитектура

### **Принципи:**
1. **Separation of Concerns** - Agreement логиката е отделно от User
2. **Audit Trail** - Всяко съгласие се записва с пълни детайли
3. **Versioning** - Проследяване на версии на споразумението
4. **GDPR Compliance** - Възможност за изтегляне и изтриване на данни

### **Нови Entity класове:**

```
cardindex.dojocardindex/
├── Agreement/
│   ├── models/
│   │   ├── Agreement.java           # ✨ НОВ
│   │   └── AgreementType.java       # ✨ НОВ (enum)
│   ├── repository/
│   │   └── AgreementRepository.java # ✨ НОВ
│   └── service/
│       └── AgreementService.java    # ✨ НОВ
│
└── UserAgreement/
    ├── models/
    │   ├── UserAgreement.java       # ✨ НОВ
    │   └── ConsentMethod.java       # ✨ НОВ (enum)
    ├── repository/
    │   └── UserAgreementRepository.java  # ✨ НОВ
    ├── service/
    │   └── UserAgreementService.java     # ✨ НОВ
    └── web/
        └── ParentConsentController.java  # ✨ НОВ
```

---

## 📝 Промени в DojoCardIndex

### **1. Промени в User Entity:**

```java
@Entity
@Table(name = "users")
public class User {
    // ...existing code...
    
    @Column(name = "birth_date", nullable = false)  // ✅ ПРОМЯНА: nullable = false
    private LocalDate birthDate;
    
    @Column(name = "contact_person")
    private String contactPerson;
    
    @Column(name = "contact_person_phone")
    private String contactPersonPhone;
    
    @Column(name = "contact_person_email")  // ✨ НОВО ПОЛЕ - nullable = TRUE
    private String contactPersonEmail;
    
    // ❌ НЕ добавяме agreed и agreementDate тук!
    // Тази информация се пази в UserAgreement entity
    
    // ...existing code...
}
```

**Обяснение на промените:**
- **`birthDate` → nullable=false**: Необходимо за проверка на възраст (дете/възрастен)
- **`contactPersonEmail` (ново)**: Email за родителско съгласие при деца
  - **nullable = true** (НЕ е задължително за възрастни!)
  - **Задължително САМО за деца (< 18)** при приемане на agreement
  - Валидация при runtime, не на database level
  
**Валидация логика:**
```java
// При приемане на agreement за дете:
if (user.getAge() < 18) {
    if (user.getContactPersonEmail() == null || user.getContactPersonEmail().isBlank()) {
        throw new ValidationException(
            "Contact person email е задължителен за потребители под 18 години!"
        );
    }
}

// Възрастните могат да нямат contactPersonEmail
```

- **Защо НЕ добавяме `agreed` в User?**
  - User може да има МНОЖЕСТВО споразумения (Terms, Liability, GDPR, Photo consent)
  - Всяко споразумение има версии
  - Audit trail изисква история на всички приемания
  - Clean separation of concerns

### **2. Нов Entity: Agreement**

```java
package cardindex.dojocardindex.Agreement.models;

import cardindex.dojocardindex.UserAgreement.models.UserAgreement;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "agreements")
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Agreement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String version;  // "1.0", "1.1", "2.0"
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;  // HTML съдържание на споразумението
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AgreementType type;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;  // От кога влиза в сила
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive;  // Само една версия е активна
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "created_by")
    private String createdBy;  // Кой админ е създал/едитнал
    
    @OneToMany(mappedBy = "agreement", cascade = CascadeType.ALL)
    private List<UserAgreement> userAgreements = new ArrayList<>();
}
```

**Обяснение:**
- **`version`**: Версия на документа (1.0, 1.1, 2.0)
  - Всяка промяна = нова версия
  - Семантично версиониране препоръчително
  
- **`content`**: HTML съдържание на споразумението
  - TEXT колона (неограничена дължина)
  - Пълен текст който user е видял и приел
  
- **`type`**: Тип на споразумението (enum)
  - TERMS_OF_SERVICE (Общи условия)
  - LIABILITY_WAIVER (Освобождаване от отговорност)
  - GDPR_CONSENT (GDPR съгласие)
  - PHOTO_CONSENT (Съгласие за снимки)
  
- **`effectiveDate`**: От кога влиза в сила
  - Позволява планиране на бъдещи версии
  - Сравнение при login проверки
  
- **`isActive`**: Само една версия е активна
  - Query: `WHERE isActive = true AND type = 'LIABILITY_WAIVER'`
  - При създаване на нова версия → стария става `isActive = false`
  
- **`createdBy`**: Audit - кой админ е създал
  - Transparency
  - Compliance

### **3. Enum: AgreementType**

```java
package cardindex.dojocardindex.Agreement.models;

public enum AgreementType {
    TERMS_OF_SERVICE,      // Общи условия за ползване
    LIABILITY_WAIVER,      // Освобождаване от отговорност (спортни наранявания)
    GDPR_CONSENT,          // GDPR съгласие за обработка на лични данни
    PHOTO_CONSENT          // Съгласие за публикуване на снимки
}
```

**Обяснение:**
- Позволява **множество типове споразумения**
- Всеки тип може да има **независими версии**
- Гъвкавост за бъдещо разширяване

### **4. Нов Entity: UserAgreement**

```java
package cardindex.dojocardindex.UserAgreement.models;

import cardindex.dojocardindex.Agreement.models.Agreement;
import cardindex.dojocardindex.User.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_agreements",
       indexes = {
           @Index(name = "idx_user_agreement", columnList = "user_id,agreement_id"),
           @Index(name = "idx_parent_token", columnList = "parent_consent_token")
       })
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserAgreement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "agreement_id", nullable = false)
    private Agreement agreement;
    
    // ════════════════════════════════════════════════════════════
    // USER CONSENT DATA
    // ════════════════════════════════════════════════════════════
    
    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;  // Кога потребителят се опита да се съгласи
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;  // IP на потребителя (IPv4 or IPv6)
    
    // ════════════════════════════════════════════════════════════
    // MINOR / PARENT CONSENT DATA
    // ════════════════════════════════════════════════════════════
    
    @Column(name = "is_minor", nullable = false)
    private boolean isMinor;  // Дали е непълнолетен (< 18)
    
    @Column(name = "parent_email")
    private String parentEmail;  // Email на родител/настойник
    
    @Column(name = "parent_consent_token", unique = true)
    private String parentConsentToken;  // UUID токен за parent consent линк
    
    @Column(name = "parent_consented_at")
    private LocalDateTime parentConsentedAt;  // Кога родителят е потвърдил
    
    @Column(name = "parent_ip_address", length = 45)
    private String parentIpAddress;  // IP на родителя
    
    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;  // Кога изтича токена (48h)
    
    // ════════════════════════════════════════════════════════════
    // CONSENT METHOD & STATUS
    // ════════════════════════════════════════════════════════════
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ConsentMethod consentMethod;
    
    @Column(name = "agreement_pending", nullable = false)
    private boolean agreementPending = false;  // ✨ ВРАТИЧКА: Временно отложено подписване
    
    @Column(name = "pending_reason")
    private String pendingReason;  // Причина за отлагане (напр. "Родител без email")
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    // ════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ════════════════════════════════════════════════════════════
    
    /**
     * Проверка дали споразумението е напълно прието
     * - За възрастни: agreedAt != null
     * - За деца: agreedAt != null && parentConsentedAt != null
     * - Agreement pending: agreementPending = true (временно валидно, чака оправяне)
     */
    public boolean isFullyAgreed() {
        // ✅ ВРАТИЧКА: Временно отложено - детето може да тренира, но трябва да се оправи
        if (agreementPending) {
            return true;
        }
        
        if (!isMinor) {
            return agreedAt != null;
        }
        return agreedAt != null && parentConsentedAt != null;
    }
    
    /**
     * Проверка дали parent consent токенът е валиден
     */
    public boolean isTokenValid() {
        if (tokenExpiresAt == null) return false;
        return LocalDateTime.now().isBefore(tokenExpiresAt);
    }
    
    /**
     * Проверка дали чака parent consent
     */
    public boolean isPendingParentConsent() {
        return isMinor && 
               agreedAt != null && 
               parentConsentedAt == null && 
               isTokenValid();
    }
}
```

**Обяснение на полетата:**

#### **User Consent Data:**
- **`agreedAt`**: Кога user-ът е кликнал "Съгласен съм"
  - Null = не е приел
  - Non-null = видял и кликнал
  
- **`ipAddress`**: IP адрес на user-а
  - Доказателство за местоположението
  - Audit trail
  - VARCHAR(45) - поддържа IPv6

#### **Minor / Parent Consent Data:**
- **`isMinor`**: Флаг дали е непълнолетен
  - Изчислява се от `birthDate`
  - Определя дали е нужно parent consent
  
- **`parentEmail`**: Email на родител/настойник
  - Копира се от `User.contactPersonEmail`
  - Изпраща се линк на този email
  
- **`parentConsentToken`**: UUID токен за верификация
  - Генерира се при създаване на запис
  - Уникален (unique constraint)
  - Линк: `https://dojo.com/parent-consent/verify?token={UUID}`
  
- **`parentConsentedAt`**: Timestamp на родителско потвърждение
  - Null = чака потвърждение
  - Non-null = потвърдено
  
- **`parentIpAddress`**: IP на родителя
  - Отделен от IP на детето
  - Доказателство че различно лице е потвърдило
  
- **`tokenExpiresAt`**: Изтичане на токена
  - Default: 48 часа
  - След изтичане → нов токен

#### **Consent Method:**
- **`consentMethod`**: Как е дадено съгласието
  - `DIRECT` - Възрастен директно се съгласява
  - `PARENT_EMAIL` - Родител чрез email линк

**Забележка:** `ConsentMethod.ADMIN` е премахнат. Вместо това използваме `agreementPending` boolean flag.

#### **Agreement Pending (Вратичка):**
- **`agreementPending`**: Boolean flag - временно отложено подписване
  - `false` (default) - Нормална проверка на съгласие
  - `true` - Подписването е отложено временно, user може да тренира
  - **Use case:** Родител няма email, технически проблем, legacy import
  - Admin може да активира споразумението по всяко време
  - Monthly reminder напомня на admin за pending agreements
  
- **`pendingReason`**: Текстово обяснение защо е отложено
  - "Родител без email"
  - "Legacy import - ще се оправи"
  - "Временен достъп - родител не знае парола"

#### **Indexes:**
```sql
-- Бърза проверка за user + agreement
CREATE INDEX idx_user_agreement ON user_agreements(user_id, agreement_id);

-- Бърза проверка на parent consent token
CREATE INDEX idx_parent_token ON user_agreements(parent_consent_token);
```

### **5. Enum: ConsentMethod**

```java
package cardindex.dojocardindex.UserAgreement.models;

public enum ConsentMethod {
    DIRECT,          // Пълнолетен потребител директно се съгласява
    PARENT_EMAIL     // Родител потвърждава чрез email линк
    
    // ЗАБЕЛЕЖКА: ADMIN е премахнат - използваме adminOverride boolean вместо това
}
```

### **6. AgreementRepository**

```java
package cardindex.dojocardindex.Agreement.repository;

import cardindex.dojocardindex.Agreement.models.Agreement;
import cardindex.dojocardindex.Agreement.models.AgreementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgreementRepository extends JpaRepository<Agreement, UUID> {
    
    /**
     * Взема активното споразумение по тип
     */
    Optional<Agreement> findByTypeAndIsActiveTrue(AgreementType type);
    
    /**
     * Всички версии на споразумение по тип (за history)
     */
    List<Agreement> findByTypeOrderByEffectiveDateDesc(AgreementType type);
}
```

### **7. UserAgreementRepository**

```java
package cardindex.dojocardindex.UserAgreement.repository;

import cardindex.dojocardindex.Agreement.models.Agreement;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.UserAgreement.models.UserAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserAgreementRepository extends JpaRepository<UserAgreement, UUID> {
    
    /**
     * Намира конкретен UserAgreement по user и agreement
     */
    Optional<UserAgreement> findByUserAndAgreement(User user, Agreement agreement);
    
    /**
     * Всички споразумения на user (за audit)
     */
    List<UserAgreement> findByUserOrderByCreatedAtDesc(User user);
    
    /**
     * Намира по parent consent token
     */
    Optional<UserAgreement> findByParentConsentToken(String token);
    
    /**
     * Проверка дали user има валидно споразумение за тип
     */
    @Query("SELECT ua FROM UserAgreement ua " +
           "WHERE ua.user = :user " +
           "AND ua.agreement.type = :type " +
           "AND ua.agreement.isActive = true")
    Optional<UserAgreement> findActiveUserAgreementByType(User user, AgreementType type);
    
    /**
     * Всички pending parent consents (за admin панел)
     */
    @Query("SELECT ua FROM UserAgreement ua " +
           "WHERE ua.isMinor = true " +
           "AND ua.agreedAt IS NOT NULL " +
           "AND ua.parentConsentedAt IS NULL " +
           "AND ua.tokenExpiresAt > CURRENT_TIMESTAMP")
    List<UserAgreement> findPendingParentConsents();
}
```

---

## 🔄 Workflow схеми

### **Workflow 1: Възрастен потребител (18+ години)**

```
┌─────────────────────────────────────────────────────────┐
│ 1. User се регистрира (email + password)               │
│    Admin въвежда birthDate                             │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 2. User влиза в системата за първи път                 │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 3. LoginInterceptor проверка:                          │
│    - User.registrationStatus == NOT_REGISTERED?        │
│      → ДА: SKIP agreement check (вратичка за admin)    │
│    - hasValidAgreement(userId, LIABILITY_WAIVER)?      │
│      → НЕ: Redirect към /agreement-required            │
│    - Резултат: false (няма UserAgreement запис)        │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 4. Redirect към /agreement-required                     │
│    Показва страница със споразумението                 │
│    - Текст на Agreement                                 │
│    - Checkbox "Прочетох и съм съгласен"                │
│    - Button "Потвърди"                                  │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 5. User кликва "Потвърди"                              │
│    POST /agreement/accept                               │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 6. UserAgreementService създава запис:                 │
│    UserAgreement {                                      │
│      user: User                                         │
│      agreement: Agreement (активна версия)              │
│      agreedAt: LocalDateTime.now()                      │
│      ipAddress: request.getRemoteAddr()                 │
│      isMinor: false                                     │
│      consentMethod: DIRECT                              │
│    }                                                     │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 7. Redirect към /home                                   │
│    User може да ползва системата                        │
└─────────────────────────────────────────────────────────┘
```

---

### **Workflow 2: Непълнолетен потребител (< 18 години)**

```
┌─────────────────────────────────────────────────────────┐
│ 1. User се регистрира (email + password)               │
│    Admin въвежда:                                       │
│      - birthDate (показва < 18 години)                  │
│      - contactPersonEmail                               │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 2. User влиза в системата за първи път                 │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 3. LoginInterceptor проверка:                          │
│    - hasValidAgreement(userId)?                         │
│    - Резултат: false                                    │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 4. Redirect към /agreement-required                     │
│    Показва споразумението                               │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 5. User кликва "Потвърди"                              │
│    POST /agreement/accept                               │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 6. Backend проверява birthDate:                        │
│    - age < 18 → isMinor = true                         │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 7. UserAgreementService:                               │
│    a) Създава UserAgreement:                           │
│       - agreedAt: now()                                 │
│       - isMinor: true                                   │
│       - parentEmail: user.contactPersonEmail            │
│       - parentConsentToken: UUID.randomUUID()           │
│       - tokenExpiresAt: now() + 48 hours                │
│       - parentConsentedAt: null (чака)                  │
│       - consentMethod: PARENT_EMAIL                     │
│                                                          │
│    b) Изпраща email на contactPersonEmail:             │
│       ForgottenPasswordRequest {                        │
│         recipient: user.contactPersonEmail              │
│         title: "Родителско съгласие..."                │
│         content: HTML с линк                            │
│       }                                                  │
│       Линк: /parent-consent/verify?token={UUID}        │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 8. Redirect към /agreement/pending-parent               │
│    Показва:                                             │
│    "Изпратихме email на родител/настойник.             │
│     Моля изчакайте потвърждение."                       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 9. Родителят получава email и кликва линка             │
│    GET /parent-consent/verify?token={UUID}              │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 10. ParentConsentController:                           │
│     - Намира UserAgreement по token                     │
│     - Проверява дали token е валиден (not expired)     │
│     - Показва страница:                                 │
│       * Име на детето                                   │
│       * Пълен текст на Agreement                        │
│       * Checkbox "Съгласен съм"                        │
│       * Button "Потвърди"                              │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 11. Родителят кликва "Потвърди"                        │
│     POST /parent-consent/confirm                        │
│     Body: { token: UUID }                               │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 12. UserAgreementService:                              │
│     - UPDATE UserAgreement:                             │
│       parentConsentedAt = now()                         │
│       parentIpAddress = request.getRemoteAddr()         │
│                                                          │
│     - UPDATE User:                                      │
│       registrationStatus = PENDING_ADMIN                │
│                                                          │
│     - Изпраща notification до Admin                     │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 13. Показва success страница на родителя               │
│     "Благодарим! Регистрацията ще бъде разгледана."    │
└─────────────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 14. При следващ login на детето:                       │
│     - hasValidAgreement() → true (има parentConsentedAt)│
│     - Може да влезе в /home                            │
└─────────────────────────────────────────────────────────┘
```

---

### **Workflow 3: Промяна на Agreement (нова версия)**

```
┌─────────────────────────────────────────────────────────┐
│ 1. Admin едитва споразумението                         │
│    POST /admin/agreement/publish                        │
│    Body: { content: HTML, version: "2.0" }             │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 2. AgreementService.publishNewAgreement():             │
│    a) Намира текущия активен Agreement                 │
│       oldAgreement.isActive = false                     │
│       agreementRepository.save(oldAgreement)            │
│                                                          │
│    b) Създава нов Agreement:                           │
│       Agreement {                                       │
│         version: "2.0"                                  │
│         content: новия HTML                             │
│         effectiveDate: LocalDate.now()                  │
│         isActive: true                                  │
│         type: LIABILITY_WAIVER                          │
│       }                                                  │
│       agreementRepository.save(newAgreement)            │
│                                                          │
│    c) ❌ НЕ триe стари UserAgreement записи!           │
│       (те остават за audit trail)                       │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 3. (Optional) Изпраща email на всички users:           │
│    "Обновихме споразумението. Моля приемете новата     │
│     версия при следващия login."                        │
└─────────────────────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 4. User се логва:                                       │
│    LoginInterceptor проверява:                          │
│    - activeAgreement = findByTypeAndIsActiveTrue()      │
│    - userAgreement = findActiveUserAgreementByType()    │
│                                                          │
│    - Ако userAgreement.agreement.id != activeAgreement.id│
│      → НЯМА валидно споразумение                        │
│      → Redirect към /agreement-required                 │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│ 5. User вижда новото споразумение и потвърждава        │
│    Създава се НОВ UserAgreement запис с новото         │
│    Agreement ID                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 💾 База данни

### **Таблица: agreements**

```sql
CREATE TABLE agreements (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    version           VARCHAR(50) NOT NULL,
    content           TEXT NOT NULL,
    type              VARCHAR(50) NOT NULL,  -- TERMS_OF_SERVICE, LIABILITY_WAIVER, etc.
    effective_date    DATE NOT NULL,
    is_active         BOOLEAN NOT NULL DEFAULT false,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by        VARCHAR(255),
    
    CONSTRAINT uk_type_version UNIQUE (type, version)
);

-- Index за бърза проверка на активното споразумение
CREATE INDEX idx_agreements_active ON agreements(type, is_active) WHERE is_active = true;
```

### **Таблица: user_agreements**

```sql
CREATE TABLE user_agreements (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    agreement_id            UUID NOT NULL REFERENCES agreements(id),
    
    -- User consent
    agreed_at               TIMESTAMP,
    ip_address              VARCHAR(45),
    
    -- Minor / Parent consent
    is_minor                BOOLEAN NOT NULL DEFAULT false,
    parent_email            VARCHAR(255),
    parent_consent_token    VARCHAR(255) UNIQUE,
    parent_consented_at     TIMESTAMP,
    parent_ip_address       VARCHAR(45),
    token_expires_at        TIMESTAMP,
    
    -- Method & metadata
    consent_method          VARCHAR(50) NOT NULL,  -- DIRECT, PARENT_EMAIL, ADMIN
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT uk_user_agreement UNIQUE (user_id, agreement_id)
);

-- Indexes
CREATE INDEX idx_user_agreements_user ON user_agreements(user_id);
CREATE INDEX idx_user_agreements_token ON user_agreements(parent_consent_token) 
    WHERE parent_consent_token IS NOT NULL;
CREATE INDEX idx_pending_consents ON user_agreements(is_minor, parent_consented_at, token_expires_at)
    WHERE is_minor = true AND parent_consented_at IS NULL;
```

### **Промени в таблица: users**

```sql
-- Добавяне на нови колони
ALTER TABLE users 
    ADD COLUMN contact_person_email VARCHAR(255) NOT NULL DEFAULT '';

-- Промяна на nullable constraint
ALTER TABLE users 
    ALTER COLUMN birth_date SET NOT NULL;
```

---

## 📊 Audit Trail

### **Какво пазим за доказателство:**

#### **За Adult Users:**
```json
{
  "userId": "550e8400-...",
  "userEmail": "user@example.com",
  "firstName": "Иван",
  "lastName": "Петров",
  "birthDate": "1995-03-15",
  "agreement": {
    "id": "660f9500-...",
    "version": "2.0",
    "type": "LIABILITY_WAIVER",
    "effectiveDate": "2026-03-01",
    "content": "<p>Пълен HTML текст...</p>"
  },
  "consent": {
    "agreedAt": "2026-03-05T14:23:45",
    "ipAddress": "192.168.1.100",
    "consentMethod": "DIRECT",
    "isMinor": false
  }
}
```

#### **За Minor Users:**
```json
{
  "userId": "770e8400-...",
  "userEmail": "child@example.com",
  "firstName": "Мария",
  "lastName": "Иванова",
  "birthDate": "2015-06-20",
  "agreement": {
    "id": "660f9500-...",
    "version": "2.0",
    "type": "LIABILITY_WAIVER",
    "effectiveDate": "2026-03-01",
    "content": "<p>Пълен HTML текст...</p>"
  },
  "consent": {
    "agreedAt": "2026-03-05T10:15:30",
    "ipAddress": "192.168.1.105",
    "isMinor": true,
    "parentEmail": "parent@example.com",
    "parentConsentToken": "880e8400-...",
    "parentConsentedAt": "2026-03-05T18:45:22",
    "parentIpAddress": "89.26.45.123",
    "consentMethod": "PARENT_EMAIL"
  }
}
```

### **Query за генериране на audit report:**

```java
@Service
public class AuditReportService {
    
    public AuditReport generateUserConsentReport(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow();
        
        List<UserAgreement> agreements = userAgreementRepository
            .findByUserOrderByCreatedAtDesc(user);
        
        return AuditReport.builder()
            .user(user)
            .agreements(agreements)
            .generatedAt(LocalDateTime.now())
            .build();
    }
    
    public byte[] generatePdfProof(UUID userId) {
        AuditReport report = generateUserConsentReport(userId);
        // Generate PDF using iText or similar
        return pdfGenerator.generate(report);
    }
}
```

---

## 📧 Промени в mail-svc

### **Опция A: БЕЗ промени (препоръчано за бързо внедряване)**

**Използваме съществуващия `/forgotten-password` endpoint:**

```java
// В DojoCardIndex - UserAgreementService
ForgottenPasswordRequest request = ForgottenPasswordRequest.builder()
    .recipient(user.getContactPersonEmail())
    .title("Родителско съгласие за регистрация")
    .content(buildParentConsentEmail(user, token))
    .build();

notificationClient.sendForgottenPasswordEmail(request);
```

**Плюсове:**
- ✅ Работи веднага
- ✅ Нулеви промени в mail-svc
- ✅ No deployment needed

**Минуси:**
- ⚠️ Endpoint naming confusion

---

### **Опция B: Добавяне на нов endpoint (препоръчано за дългосрочно)**

#### **1. Нов model в mail-svc:**

```java
// mail-svc/src/main/java/mail_svc/model/ParentConsentRequest.java
package mail_svc.model;

import lombok.Data;

@Data
public class ParentConsentRequest {
    private String parentEmail;
    private String childFirstName;
    private String childLastName;
    private String consentLink;
}
```

#### **2. Нов метод в NotificationController:**

```java
// mail-svc/src/main/java/mail_svc/web/NotificationController.java
@PostMapping("/parent-consent")
public ResponseEntity<Void> sendParentConsentEmail(
    @RequestBody ParentConsentRequest request) {
    
    notificationService.sendParentConsentEmail(request);
    return ResponseEntity.status(HttpStatus.OK).build();
}
```

#### **3. Нов метод в NotificationService:**

```java
// mail-svc/src/main/java/mail_svc/service/NotificationService.java
public void sendParentConsentEmail(ParentConsentRequest request) {
    Context context = new Context();
    context.setVariable("childFirstName", request.getChildFirstName());
    context.setVariable("childLastName", request.getChildLastName());
    context.setVariable("consentLink", request.getConsentLink());
    
    String htmlContent = templateEngine.process("parent-consent", context);
    
    try {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(request.getParentEmail());
        helper.setSubject("Родителско съгласие за регистрация в Dojo Card Index");
        helper.setText(htmlContent, true);
        mailSender.send(message);
    } catch (MessagingException e) {
        log.warn("Изпращането на родителско съгласие до {} беше неуспешно!", 
                 request.getParentEmail(), e);
        throw new RuntimeException("Parent consent email не беше изпратен!");
    }
}
```

#### **4. Нов template:**

```html
<!-- mail-svc/src/main/resources/templates/parent-consent.html -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <title>Родителско съгласие</title>
</head>
<body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
        <h2 style="color: #d32f2f;">Родителско съгласие</h2>
        
        <p>Здравейте,</p>
        
        <p>
            <strong th:text="${childFirstName} + ' ' + ${childLastName}"></strong> 
            е направил/а регистрация в <strong>Dojo Card Index</strong>.
        </p>
        
        <p>
            За да завършите регистрацията, моля прочетете и потвърдете 
            споразумението за освобождаване от отговорност и условията за ползване.
        </p>
        
        <div style="text-align: center; margin: 30px 0;">
            <a th:href="${consentLink}" 
               style="background-color: #d32f2f; 
                      color: white; 
                      padding: 12px 30px; 
                      text-decoration: none; 
                      border-radius: 5px;
                      display: inline-block;">
                Прегледай и потвърди споразумението
            </a>
        </div>
        
        <p style="color: #666; font-size: 14px;">
            <strong>Важно:</strong> Линкът е валиден 48 часа.
        </p>
        
        <p>
            С уважение,<br/>
            <strong>Dojo Card Index Team</strong>
        </p>
    </div>
</body>
</html>
```

#### **5. Update на NotificationClient в DojoCardIndex:**

```java
// DojoCardIndex - NotificationClient.java
@FeignClient(name = "mail-svc", 
             url = "https://mail-svc-app-container-app...")
public interface NotificationClient {
    
    // ...existing methods...
    
    @PostMapping("/parent-consent")
    ResponseEntity<Void> sendParentConsentEmail(
        @RequestBody ParentConsentRequest request);
}
```

#### **6. Нов DTO в DojoCardIndex:**

```java
// DojoCardIndex - ParentConsentRequest.java
package cardindex.dojocardindex.notification.client.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ParentConsentRequest {
    private String parentEmail;
    private String childFirstName;
    private String childLastName;
    private String consentLink;
}
```

---

## 🚀 Имплементационен план

### **Фаза 1: Database & Entity Setup**

#### **Стъпка 1.1: Създаване на entities**
- [ ] Създай `Agreement` entity
- [ ] Създай `AgreementType` enum
- [ ] Създай `UserAgreement` entity
- [ ] Създай `ConsentMethod` enum

#### **Стъпка 1.2: Repositories**
- [ ] Създай `AgreementRepository`
- [ ] Създай `UserAgreementRepository`

#### **Стъпка 1.3: Промени в User**
- [ ] Добави `contactPersonEmail` поле
- [ ] Направи `birthDate` nullable=false
- [ ] Update constructor (Lombok builder ще го направи автоматично)

#### **Стъпка 1.4: Database migration**
- [ ] Flyway/Liquibase migration за нови таблици
- [ ] ИЛИ: Hibernate auto-ddl (за dev environment)

---

### **Фаза 2: Service Layer**

#### **Стъпка 2.1: AgreementService**
- [ ] `createAgreement()` - Създаване на ново споразумение
- [ ] `getActiveAgreement(AgreementType)` - Взема активното
- [ ] `publishNewVersion()` - Деактивира старо, активира ново
- [ ] `getAgreementHistory(AgreementType)` - Всички версии

#### **Стъпка 2.2: UserAgreementService**
- [ ] `hasValidAgreement(User, AgreementType)` - Проверка при login
- [ ] `acceptAgreement(User, Agreement, IP)` - Adult acceptance
- [ ] `initiateParentConsent(User, Agreement, IP)` - Minor acceptance
- [ ] `verifyParentConsent(String token, IP)` - Parent verification
- [ ] `sendParentConsentEmail(UserAgreement)` - Email изпращане

#### **Стъпка 2.3: Email Integration**
- [ ] Update `NotificationClient` (ако Опция B)
- [ ] ИЛИ: Използвай `sendForgottenPasswordEmail()` (Опция A)

---

### **Фаза 3: Web Layer**

#### **Стъпка 3.1: Controllers**
- [ ] `AgreementController` - Показване на споразумение
  - `GET /agreement-required` - Показва формата
  - `POST /agreement/accept` - Приемане
  - `GET /agreement/pending-parent` - Чака parent consent
  
- [ ] `ParentConsentController`
  - `GET /parent-consent/verify?token={UUID}` - Показва формата
  - `POST /parent-consent/confirm` - Parent потвърждава

#### **Стъпка 3.2: Admin панел**
- [ ] `AdminAgreementController`
  - `GET /admin/agreements` - Списък с версии
  - `GET /admin/agreements/new` - Форма за ново
  - `POST /admin/agreements/publish` - Публикуване

#### **Стъпка 3.3: Thymeleaf templates**
- [ ] `agreement-required.html` - User вижда споразумението
- [ ] `agreement-pending-parent.html` - Чака parent consent
- [ ] `parent-consent-verify.html` - Parent потвърждава
- [ ] `parent-consent-success.html` - Success страница

---

### **Фаза 4: Security & Validation**

#### **Стъпка 4.1: Login Interceptor**
- [ ] `AgreementCheckInterceptor` - Проверка при всеки request
  - Skip paths: `/agreement/**`, `/parent-consent/**`, `/login`, `/css/**`
  - Check: `hasValidAgreement(currentUser)`
  - Redirect: `/agreement-required` ако false

#### **Стъпка 4.2: Token Security**
- [ ] Валидация на token expiration
- [ ] Rate limiting за parent consent endpoints
- [ ] CSRF protection

---

### **Фаза 5: Admin Features**

#### **Стъпка 5.1: Agreement Management**
- [ ] CRUD операции за споразумения
- [ ] Versioning UI
- [ ] Preview преди publish

#### **Стъпка 5.2: Monitoring**
- [ ] Dashboard: Pending parent consents
- [ ] Dashboard: Users без валидно споразумение
- [ ] Expired tokens report

---

### **Фаза 6: Testing**

#### **Стъпка 6.1: Unit Tests**
- [ ] `AgreementServiceTest`
- [ ] `UserAgreementServiceTest`
- [ ] Age calculation logic
- [ ] Token validation logic

#### **Стъпка 6.2: Integration Tests**
- [ ] Full workflow: Adult acceptance
- [ ] Full workflow: Minor + parent consent
- [ ] Agreement versioning
- [ ] Email sending (mock Feign client)

---

### **Фаза 7: Documentation & Audit**

#### **Стъпка 7.1: Legal Documentation**
- [ ] Template на споразумението (BG)
- [ ] GDPR съответствие документ
- [ ] Процедури за audit

#### **Стъпка 7.2: Technical Documentation**
- [ ] API документация (Swagger)
- [ ] Database schema документация
- [ ] Deployment guide

---

## 📌 Следващи стъпки

### **✅ Immediate (Потвърдени решения):**
1. ✅ **Архитектурата е потвърдена** - Готова за имплементация
2. ✅ **Email strategy избрана:** Опция B - нов `/parent-consent` endpoint в mail-svc
3. ✅ **ConsentMethod опростен:** Само DIRECT и PARENT_EMAIL (без ADMIN)
4. ✅ **Deactivation policy:** Веднага при отказ
5. ✅ **Export формати:** Всички (CSV, Excel, PDF, JSON)

### **🚀 Имплементационен ред:**

#### **Стъпка 1: Промени в mail-svc** (External service)
```
1. Създай ParentConsentRequest.java
2. Добави POST /parent-consent endpoint в NotificationController
3. Създай sendParentConsentEmail() в NotificationService
4. Създай parent-consent.html template
5. Deploy mail-svc
6. Тества endpoint с Postman/curl
```

#### **Стъпка 2: DojoCardIndex - Backend**
```
1. Създай Agreement entity & repository
2. Създай UserAgreement entity & repository
3. Update User entity (contactPersonEmail nullable=TRUE, birthDate nullable=false)
4. Създай AgreementService
5. Създай UserAgreementService (with validation за contactPersonEmail при деца)
6. Update NotificationClient с parent-consent endpoint
```


#### **Стъпка 3: DojoCardIndex - Web Layer**
```
1. AgreementController
2. ParentConsentController
3. LoginInterceptor за agreement проверка
4. Thymeleaf templates
```

#### **Стъпка 4: Export функционалност**
```
1. CSV + Excel generators (приоритет 1)
2. AdminExportController
3. Admin UI за експорт
4. PDF + JSON generators (приоритет 2)
```

#### **Стъпка 5: Testing & Deployment**
```
1. Unit tests
2. Integration tests
3. Manual testing (full workflow)
4. Deploy
```

---

### **📝 Подготовка преди имплементация:**

#### **Необходимо за старт:**
1. 📄 **HTML текст на споразумението** (version 1.0)
   - Освобождаване от отговорност
   - Общи условия
   - GDPR клаузи
   - Подходящ за деца и възрастни

2. 🔑 **Access към mail-svc repository**
   - GitHub permissions за push
   - Deployment процедура за mail-svc

3. ✅ **Database backup** (преди migration)

---

## ✅ РЕШЕНИЯ (Потвърдени от клиента - 2026-03-05)

### **1. Agreement Types:**
**✅ РЕШЕНИЕ: ЕДНО споразумение (всичко в едно)**
- Само `AgreementType.LIABILITY_WAIVER` ще се използва
- Включва: Освобождаване от отговорност + Общи условия + GDPR в един документ
- По-просто за потребителите - един документ за приемане
- Enum `AgreementType` остава за бъдещо разширяване, но засега само LIABILITY_WAIVER

### **2. Email Strategy:**
**✅ РЕШЕНИЕ: Опция B - Добавяме нов endpoint в mail-svc**
- ✅ Ще създадем `/api/v1/notifications/parent-consent` endpoint
- ✅ Чиста архитектура - специализиран endpoint
- ✅ Dedicated template за parent consent (`parent-consent.html`)
- ✅ По-добра семантика и документация

**Промени в mail-svc (детайлно описани по-горе):**
1. Нов model: `ParentConsentRequest.java`
2. Нов endpoint в `NotificationController`: `POST /parent-consent`
3. Нов метод в `NotificationService`: `sendParentConsentEmail()`
4. Нов template: `parent-consent.html`

### **3. Admin Registration:**
**✅ РЕШЕНИЕ: User сам трябва да приеме споразумението**
- При регистрация от admin → user получава email и password
- При първия login → показва споразумението за приемане
- **Премахва се `ConsentMethod.ADMIN`** от enum (само DIRECT и PARENT_EMAIL)
- **User entity НЕ съдържа `agreed` поле** - проследява се само чрез `UserAgreement`

**Опростяване:**
- User DTO не съдържа `agreed` поле
- Проверката става чрез `UserAgreement` записи в базата
- По-чиста separation of concerns
- Admin не може да mark-не user като "agreed" - всеки user сам приема

**ConsentMethod enum (финална версия):**
```java
public enum ConsentMethod {
    DIRECT,          // Пълнолетен потребител директно се съгласява
    PARENT_EMAIL     // Родител потвърждава чрез email линк
}
```

**✅ ВРАТИЧКА ЗА ADMIN (опростено решение):**

Admin може да създаде `UserAgreement` с `agreementWaived = true` за legacy users:

```java
// В UserAgreement entity:
@Column(name = "agreement_waived", nullable = false)
private boolean agreementWaived = false;  // Споразумението е пропуснато (за legacy users)

// Проверка в AgreementService:
public boolean hasValidAgreement(User user) {
    UserAgreement ua = findUserAgreement(user, LIABILITY_WAIVER);
    if (ua == null) return false;
    
    // ✅ ВРАТИЧКА: Ако споразумението е пропуснато, приемаме като валидно
    if (ua.isAgreementWaived()) return true;
    
    // Нормална проверка
    return ua.isFullyAgreed();
}

// Admin може да създаде:
UserAgreement.builder()
    .user(childUser)
    .agreement(activeAgreement)
    .agreementPending(true)  // ✅ Временно отложено
    .pendingReason("Родител без email - ще се оправи")
    .isMinor(true)
    .parentEmail(null)
    .agreedAt(LocalDateTime.now())
    .ipAddress("ADMIN_CREATED_PENDING")
    .consentMethod(ConsentMethod.PARENT_EMAIL)
    .build();
```

**Предимства:**
- ✅ Проста логика - само един boolean flag
- ✅ **Временно решение** - може да се активира по-късно
- ✅ **Заобикаляне на липсващ email** - дете може да тренира докато се оправи
- ✅ Audit trail - вижда се причината в `pendingReason`
- ✅ Без усложняване на LoginInterceptor
- ✅ **Monthly reminder** - admin получава напомняне

### **4. Deactivation Policy:**
**✅ РЕШЕНИЕ: Веднага деактивация при отказ**
- Ако user кликне "Отказвам" → `User.status = INACTIVE` веднага
- **БЕЗ grace period**
- Admin може да реактивира акаунта при нужда (ръчно)
- Clear и строга политика - или приемаш, или акаунтът се деактивира

**Workflow при отказ:**
```
User → Вижда споразумението
  ├─ "Приемам" → UserAgreement се създава, redirect към /home
  └─ "Отказвам" → User.status = INACTIVE
                  → Logout
                  → Показва съобщение: "Акаунтът е деактивиран поради отказ 
                     на споразумението. За реактивация, свържете се с администратор."
```

### **5. Existing Users:**
**✅ РЕШЕНИЕ: При следващия login показваме споразумението**
- Всички съществуващи users без `UserAgreement` запис
- При следващия login → LoginInterceptor проверява `hasValidAgreement()`
- Ако false → redirect към `/agreement-required`
- Трябва да приемат споразумението за да продължат
- **БЕЗ bulk import** - всеки user индивидуално приема онлайн

**Migration strategy:**
```
1. Deploy новия код (с Agreement и UserAgreement entities)
2. Всички съществуващи users продължават да работят (interceptor ги пропуска временно)
3. При следващ login → показва споразумението
4. След приемане → записва се в UserAgreement
5. Gradual migration без downtime
6. След 1-2 месеца: активираме strict mode (всички ТРЯБВА да имат agreement)
```

### **6. Export формати:**
**✅ РЕШЕНИЕ: Всички формати (приоритизирани)**

**Приоритет 1 (критични за архива на клуба):**
- ✅ **CSV** - за бърза обработка в Excel
- ✅ **Excel** - с множество sheets и статистики

**Приоритет 2 (допълнителни):**
- ✅ **PDF** - индивидуални сертификати за потребители
- ✅ **JSON** - за backup и програмна обработка

**Имплементационен ред:**
1. Фаза 1: CSV + Excel (implement първо)
2. Фаза 2: PDF + JSON (implement след основната функционалност)

---

## 📝 Резюме

### **Какво правим:**
1. ✅ Създаваме **Agreement** и **UserAgreement** entities
2. ✅ Добавяме **contactPersonEmail** в User (**nullable=TRUE** - задължителен само за деца)
3. ✅ Правим **birthDate** nullable=false в User
4. ✅ Проверка при login: **hasValidAgreement()** чрез LoginInterceptor
5. ✅ **Вратички за admin**: RegistrationStatus.NOT_REGISTERED или PENDING_AGREEMENT пропускат agreement check
6. ✅ Workflow за възрастни: Директно приемане (DIRECT)
7. ✅ Workflow за деца: Email на родител → parent consent (PARENT_EMAIL)
8. ✅ Versioning на споразумения (но засега само version 1.0)
9. ✅ Audit trail - пълна история с IP, timestamps, versions
10. ✅ Export функционалност (CSV, Excel, PDF, JSON)
11. ✅ **Нов endpoint в mail-svc** за parent consent emails
12. ✅ **Runtime валидация** на contactPersonEmail за деца при приемане на agreement

### **Какво НЕ правим:**
- ❌ НЕ добавяме `agreed` в User entity (пази се само в UserAgreement)
- ❌ НЕ трием стари UserAgreement записи (audit trail е важен)
- ❌ НЕ клонираме mail-svc в този проект (отделен сървис)
- ❌ НЕ имплементираме ConsentMethod.ADMIN (всеки user приема онлайн)
- ❌ НЕ даваме grace period при отказ (веднага деактивация)
- ❌ НЕ правим bulk import на хартиени документи (всеки приема онлайн)
- ❌ НЕ правим contactPersonEmail nullable=false на DB level (само за деца е нужен)

### **Опростявания:**
- 🎯 **Само едно споразумение** (LIABILITY_WAIVER) - включва всичко в един документ
- 🎯 **Само два ConsentMethod** (DIRECT, PARENT_EMAIL) - без ADMIN
- 🎯 **User DTO без `agreed`** - проследяване само чрез UserAgreement entity
- 🎯 **Strict deactivation** - без grace period, ясна политика
- 🎯 **contactPersonEmail nullable** - задължителен само за деца (runtime validation)

### **Вратичка за гъвкавост:**
- 🚪 **UserAgreement.agreementPending** (boolean) - временно отложено подписване
  - Прост boolean flag вместо сложна RegistrationStatus логика
  - **Временно решение** - не е permanent, може да се активира
  - **Use case:** Родител няма email, legacy import, технически проблем
  - **Monthly reminder:** Admin получава напомняне на 1-во число
  - Audit trail - вижда се причината в `pendingReason`
- 🚪 **contactPersonEmail nullable** - възрастни НЕ са задължени да го имат

### **Правна валидност:**
- ✅ Доказателство: **WHO** (user ID, email), **WHEN** (timestamps), **WHAT** (agreement version + content), **WHERE** (IP addresses), **HOW** (consent method)
- ✅ Съхранение: 5+ години след прекратяване на членство
- ✅ GDPR: Право на достъп, изтриване, коригиране, export
- ✅ Родителско съгласие: Email верификация + отделен timestamp + отделен IP
- ✅ Export за архива на клуба: CSV, Excel, PDF, JSON

### **Архитектурни решения:**
- 📦 **Отделни entities** за Agreement и UserAgreement (не в User)
- 🔗 **ManyToOne** връзки за history tracking
- 🔐 **Token-based** parent consent verification (48h expiration)
- 📧 **Dedicated endpoint** в mail-svc за parent emails
- 🚫 **LoginInterceptor** за автоматична проверка при всеки request
- 📊 **Export service** с filter capabilities

---

**Автор:** GitHub Copilot  
**Дата:** 2026-03-05  
**Статус:** ✅ ПОТВЪРДЕНО - Готово за имплементация

**Решения потвърдени:**
1. ✅ Едно споразумение (LIABILITY_WAIVER)
2. ✅ Нов endpoint в mail-svc (`/parent-consent`)
3. ✅ User сам приема (без ADMIN consent)
4. ✅ Веднага деактивация при отказ
5. ✅ Съществуващи users приемат при следващ login
6. ✅ Всички export формати (CSV, Excel, PDF, JSON)

**Следващо действие:** 🚀 Започваме имплементацията! 

**Имплементационен ред:**
1. Промени в mail-svc (нов endpoint)
2. Entities в DojoCardIndex (Agreement, UserAgreement)
3. Services
4. Controllers & UI
5. Export функционалност

🥋 **Ready to code!**

---

## 📚 Допълнителни документи

### **Създадени файлове:**

1. **`user-agreement-implementation-plan.md`** (този файл)
   - Пълна документация на архитектурата
   - Детайлни code examples
   - Workflow диаграми
   - Database schema
   - **~1700 реда** - Reference документ

2. **`user-agreement-quick-summary.md`** ✨ НОВО
   - Кратко резюме на решенията
   - Quick reference таблици
   - Simplified workflows
   - Checklist преди старт
   - **~250 реда** - Daily reference

3. **`agreement-export-implementation.md`**
   - Детайлна документация на export функционалността
   - Примери на всички export формати (CSV, Excel, PDF, JSON)
   - Пълен код на generators
   - API endpoints
   - **~1000 реда** - Export reference

### **Кой файл за какво:**

| Цел | Файл |
|-----|------|
| Преглед на архитектурата | `user-agreement-implementation-plan.md` |
| Бърз преглед на решенията | `user-agreement-quick-summary.md` ⚡ |
| Export детайли | `agreement-export-implementation.md` |
| mail-svc код | GitHub: `mihaylov79/mail-svc` |

---

## 🎉 Завършено!

Документацията е **потвърдена** и **готова за имплементация**.

**Всички решения са взети:**
- ✅ Едно споразумение (LIABILITY_WAIVER)
- ✅ Нов endpoint в mail-svc
- ✅ User сам приема (без ADMIN)
- ✅ Веднага deactivate при отказ
- ✅ Съществуващи users приемат при login
- ✅ Всички export формати

**Следващо действие:** Започни с имплементацията! 🚀

**Препоръчан ред:**
1. mail-svc промени първо (external dependency)
2. DojoCardIndex entities
3. Services & Controllers
4. UI & Testing
5. Export functionality

---

**Документацията е финализирана на:** 2026-03-05  
**Потвърдена от:** Клиент  
**Prepared by:** GitHub Copilot  
**Status:** ✅ READY FOR IMPLEMENTATION

