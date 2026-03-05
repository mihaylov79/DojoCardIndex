# User Agreement Implementation - Quick Summary
## Кратки решения за имплементацията

**Дата:** 2026-03-05  
**Статус:** ✅ ПОТВЪРДЕНО

---

## ✅ Ключови решения

### **1. Agreement Types**
**Решение:** ЕДНО споразумение
- Само `AgreementType.LIABILITY_WAIVER`
- Включва: Освобождаване от отговорност + Общи условия + GDPR
- По-просто за users

### **2. Email Strategy**
**Решение:** Нов endpoint в mail-svc
- ❌ **НЕ** използваме `/forgotten-password`
- ✅ **ДА** създаваме `/api/v1/notifications/parent-consent`
- Чиста архитектура, dedicated template

### **3. Admin Registration**
**Решение:** User сам приема + временно отлагане
- ❌ Admin **НЕ** може да mark-не като "agreed"
- ✅ User трябва сам да приеме при login
- ❌ **Премахваме** `ConsentMethod.ADMIN`
- ✅ **ВРАТИЧКА**: Admin може да създаде `UserAgreement` с `agreementPending = true`
  - Временно отложено подписване (не е permanent)
  - Use case: Родител няма email, legacy import
  - Admin може да активира по всяко време
  - Monthly reminder на 1-во число

### **4. Deactivation Policy**
**Решение:** Веднага деактивация
- ❌ **БЕЗ** grace period
- ✅ Веднага `User.status = INACTIVE` при отказ
- Admin може да реактивира ръчно

### **5. Existing Users**
**Решение:** Приемат при следващ login
- ✅ При login → показва споразумението
- ❌ **БЕЗ** bulk import на хартиени документи
- Gradual migration

### **6. Export Formats**
**Решение:** Всички формати
- **Приоритет 1:** CSV + Excel
- **Приоритет 2:** PDF + JSON

---

## 🏗️ Архитектура (Опростена)

### **Entities:**

```
Agreement
├── id (UUID)
├── version (String)
├── content (TEXT)
├── type (AgreementType) → Само LIABILITY_WAIVER
├── effectiveDate (LocalDate)
└── isActive (boolean)

UserAgreement
├── id (UUID)
├── user (FK)
├── agreement (FK)
├── agreedAt (LocalDateTime)
├── ipAddress (String)
├── isMinor (boolean)
├── parentEmail (String)
├── parentConsentedAt (LocalDateTime)
├── parentIpAddress (String)
├── consentMethod (Enum) → Само DIRECT или PARENT_EMAIL
├── agreementPending (boolean) → ✨ ВРАТИЧКА (временно отложено подписване)
├── pendingReason (String) → Причина за отлагане
└── tokenExpiresAt (LocalDateTime)

User (промени)
├── contactPersonEmail (String, nullable=TRUE) ✨ НОВО - задължителен САМО за деца
└── birthDate (LocalDate, nullable=false) ✅ ПРОМЯНА
```

### **ConsentMethod Enum:**

```java
public enum ConsentMethod {
    DIRECT,          // Възрастен (18+)
    PARENT_EMAIL     // Дете (< 18) + parent consent
    // ADMIN - ПРЕМАХНАТ
}
```

---

## 📧 Mail-svc Промени

### **Нов endpoint:**

```http
POST /api/v1/notifications/parent-consent
Content-Type: application/json

{
  "parentEmail": "parent@example.com",
  "childFirstName": "Мария",
  "childLastName": "Иванова",
  "consentLink": "https://dojo.com/parent-consent/verify?token=abc-123"
}
```

### **Нов model:**

```java
// ParentConsentRequest.java
@Data
public class ParentConsentRequest {
    private String parentEmail;
    private String childFirstName;
    private String childLastName;
    private String consentLink;
}
```

### **Нов template:**

```html
<!-- parent-consent.html -->
<p><strong>{{childFirstName}} {{childLastName}}</strong> е направил/а регистрация.</p>
<a href="{{consentLink}}">Прегледай и потвърди споразумението</a>
```

---

## 🔄 Workflows (Опростени)


### **contactPersonEmail Validation:**
```java
// При приемане на agreement за дете:
if (user.getAge() < 18) {
    if (contactPersonEmail == null || contactPersonEmail.isBlank()) {
        throw new ValidationException("Contact person email е задължителен за деца!");
    }
}
// Възрастни могат да НЯМАТ contactPersonEmail
```

### **Adult User (18+):**

```
Login → hasValidAgreement()?
  ├─ YES (има UserAgreement с isFullyAgreed() == true) → /home
  │   └─ agreementWaived == true? → винаги валидно (споразумението е пропуснато)
  └─ NO → /agreement-required
            ├─ "Приемам" → UserAgreement (DIRECT) → /home
            └─ "Отказвам" → User.status = INACTIVE → Logout
```

**Admin може да създаде:**
```java
// За legacy user без реално съгласие:
UserAgreement.builder()
    .user(legacyUser)
    .agreement(activeAgreement)
    .agreedAt(LocalDateTime.now())
    .ipAddress("ADMIN_CREATED")
    .consentMethod(ConsentMethod.DIRECT)
    .agreementWaived(true)  // ✅ Споразумението е пропуснато
    .build();
```

### **Minor User (< 18):**

```
Login → hasValidAgreement()?
  └─ NO → /agreement-required
            ├─ "Приемам" → UserAgreement (PARENT_EMAIL, pending)
            │             → Email на родител
            │             → /agreement/pending-parent
            │
            └─ "Отказвам" → User.status = INACTIVE → Logout

Родител → Клик на линк → /parent-consent/verify?token=xxx
  ├─ "Потвърди" → parentConsentedAt = now()
  │             → User може да влиза
  └─ Token изтича след 48h
```

---

## 📊 Export Функционалност

### **Endpoints:**

```http
GET /admin/export/agreements/csv?dateFrom=...&dateTo=...
GET /admin/export/agreements/excel
GET /admin/export/agreements/json
GET /admin/export/pending-consents/csv
GET /admin/export/user/{userId}/certificate/pdf
```

### **Филтри:**

```
- dateFrom / dateTo
- agreementType (засега само LIABILITY_WAIVER)
- version
- isMinor (true/false)
- onlyPending (true/false)
```

### **Export формати:**

| Формат | Use Case | Приоритет |
|--------|----------|-----------|
| CSV | Excel обработка | 1 |
| Excel | Multiple sheets + stats | 1 |
| PDF | Индивидуален сертификат | 2 |
| JSON | Backup / API | 2 |

---

## 🚀 Имплементационен ред

### **Стъпка 1: mail-svc** (External)
1. ParentConsentRequest.java
2. POST /parent-consent endpoint
3. sendParentConsentEmail() метод
4. parent-consent.html template
5. Deploy + test

### **Стъпка 2: DojoCardIndex Entities**
1. Agreement entity + repo
2. UserAgreement entity + repo
3. Update User (contactPersonEmail, birthDate nullable=false)
4. Database migration

### **Стъпка 3: Services**
1. AgreementService
2. UserAgreementService
3. Update NotificationClient

### **Стъпка 4: Web Layer**
1. AgreementController
2. ParentConsentController
3. LoginInterceptor
4. Thymeleaf templates

### **Стъпка 5: Export**
1. CSV + Excel generators
2. AdminExportController
3. Admin UI
4. PDF + JSON (later)

### **Стъпка 6: Testing**
1. Unit tests
2. Integration tests
3. Manual testing
4. Deploy

---

## ❌ Какво НЕ правим

| Действие | Решение |
|----------|---------|
| Добавяме `agreed` в User entity | ❌ НЕ - само в UserAgreement |
| ConsentMethod.ADMIN | ❌ НЕ - използваме `agreementPending` boolean |
| Grace period при отказ | ❌ НЕ - веднага deactivate |
| Bulk import хартиени документи | ❌ НЕ - admin създава UserAgreement с agreementPending=true |
| Използване на `/forgotten-password` | ❌ НЕ - нов endpoint |
| Множество типове agreements | ❌ НЕ - само LIABILITY_WAIVER |
| contactPersonEmail nullable=false | ❌ НЕ - nullable=TRUE (само за деца е нужен) |
| Усложняване на RegistrationStatus | ❌ НЕ - прост agreementPending boolean |
| Permanent exemption | ❌ НЕ - agreementPending е **временно**, може да се активира |

## ✅ Вратички за гъвкавост

| Вратичка | Как работи | Use Case |
|----------|------------|----------|
| `UserAgreement.agreementWaived = true` | `isFullyAgreed()` винаги връща `true` | Legacy users, хартиени документи |
| `contactPersonEmail = null` OK | Възрастни НЕ са задължени да го имат | Само деца изискват parent email |

**Предимства на agreementWaived:**
- ✅ Прост boolean flag
- ✅ Ясна семантика - "споразумението е пропуснато"
- ✅ Audit trail (вижда се в базата)
- ✅ Без усложняване на LoginInterceptor
- ✅ Без промени в RegistrationStatus

---

## 📝 Checklist преди старт

- [ ] HTML текст на споразумението (version 1.0) готов
- [ ] Access към mail-svc repository (GitHub permissions)
- [ ] Database backup направен
- [ ] mail-svc deployment процедура известна
- [ ] Thymeleaf templates design готов (или wireframes)
- [ ] Agreement архива локация определена (за export файлове)

---

## 🎯 Success Criteria

### **Must Have:**
- ✅ Adult users могат да приемат споразумението
- ✅ Minor users получават email на родител
- ✅ Parent може да потвърди чрез линк
- ✅ При отказ → веднага deactivate
- ✅ CSV + Excel export работи
- ✅ LoginInterceptor проверява agreement

### **Nice to Have:**
- ✅ PDF сертификати
- ✅ JSON export
- ✅ Admin dashboard за pending consents
- ✅ Email notification при нова версия

---

## 📞 Контакти / Resources

- **Main Plan:** `user-agreement-implementation-plan.md` (пълен документ)
- **Export Details:** `agreement-export-implementation.md`
- **mail-svc Repo:** https://github.com/mihaylov79/mail-svc
- **DojoCardIndex Repo:** (локален проект)

---

**Ready to implement!** 🥋🚀

---

**Последна актуализация:** 2026-03-05  
**Потвърдено от:** Клиент  
**Подготвено от:** GitHub Copilot

