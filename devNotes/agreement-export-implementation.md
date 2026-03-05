# Agreement Export Implementation
## Детайлна документация за експорт на потребителски съгласия

**Дата:** 2026-03-05  
**Статус:** Планиране  
**Цел:** Експорт на всички съгласия за архива на клуба

---

## 📋 Съдържание
1. [Бизнес изисквания](#бизнес-изисквания)
2. [Export формати](#export-формати)
3. [Backend архитектура](#backend-архитектура)
4. [Пълен код за имплементация](#пълен-код-за-имплементация)
5. [API документация](#api-документация)
6. [UI компоненти](#ui-компоненти)
7. [Testing](#testing)

---

## 🎯 Бизнес изисквания

### **Защо е необходимо:**
- **Архив на клуба** - Физическо съхранение на електронни доказателства
- **Правна защита** - Доказателство при спорове
- **Audit compliance** - Проверки от държавни органи
- **Backup** - Резервно копие извън базата данни
- **Анализ** - Excel обработка на статистики

### **Какво трябва да може да се експортира:**

| Информация | Обяснение |
|------------|-----------|
| User данни | Име, email, дата на раждане |
| Agreement данни | Тип, версия, съдържание |
| Consent данни | Дата, IP адрес, метод |
| Parent consent | Email, дата, IP на родител |
| Статус | Completed / Pending / Expired |

---

## 📄 Export формати

### **1. CSV - Comma Separated Values**

**Предимства:**
- ✅ Може да се отвори директно в Excel
- ✅ Лесно четим
- ✅ Малък размер на файла
- ✅ Поддържа Unicode (UTF-8)

**Недостатъци:**
- ❌ Само една таблица
- ❌ Без форматиране (bold, colors)

**Use case:** 
- Експорт за бърза обработка в Excel
- Import в други системи

**Пример файл:** `user-agreements-export-2026-03-05.csv`

```csv
User ID,Email,First Name,Last Name,Birth Date,Age,Agreement Type,Agreement Version,Agreed At,IP Address,Is Minor,Parent Email,Parent Consented At,Parent IP,Consent Method,Status
550e8400-e29b-41d4-a716-446655440000,ivan@example.com,Иван,Петров,1995-03-15,30,LIABILITY_WAIVER,2.0,2026-03-05 14:23:45,192.168.1.100,false,,,,,DIRECT,Completed
770e8400-e29b-41d4-a716-446655440001,maria@example.com,Мария,Иванова,2015-06-20,10,LIABILITY_WAIVER,2.0,2026-03-05 10:15:30,192.168.1.105,true,parent@example.com,2026-03-05 18:45:22,89.26.45.123,PARENT_EMAIL,Completed
880e8400-e29b-41d4-a716-446655440002,petar@example.com,Петър,Димитров,2016-08-10,9,LIABILITY_WAIVER,2.0,2026-03-04 09:30:00,192.168.1.110,true,dad@example.com,,,PARENT_EMAIL,Pending Parent
990e8400-e29b-41d4-a716-446655440003,georgi@example.com,Георги,Петров,2000-12-25,25,LIABILITY_WAIVER,1.0,2025-10-15 11:20:00,192.168.1.120,false,,,,,ADMIN,Completed
```

---

### **2. Excel - XLSX формат**

**Предимства:**
- ✅ Множество sheets (All / Pending / Statistics)
- ✅ Форматиране (bold headers, colors)
- ✅ Формули и изчисления
- ✅ Freeze panes за по-добра навигация

**Недостатъци:**
- ❌ По-голям файл
- ❌ Изисква специални библиотеки (Apache POI)

**Use case:**
- Детайлен analysis
- Презентация на статистики
- Professional отчети

**Файл структура:** `user-agreements-export-2026-03-05.xlsx`

#### **Sheet 1: All Agreements**
| User | Email | Age | Agreement | Version | Agreed Date | Parent Consent | Status |
|------|-------|-----|-----------|---------|-------------|----------------|--------|
| Иван Петров | ivan@example.com | 30 | LIABILITY_WAIVER | 2.0 | 05.03.2026 14:23 | N/A | ✅ Completed |
| Мария Иванова | maria@example.com | 10 | LIABILITY_WAIVER | 2.0 | 05.03.2026 10:15 | ✅ 05.03.2026 18:45 | ✅ Completed |
| Петър Димитров | petar@example.com | 9 | LIABILITY_WAIVER | 2.0 | 04.03.2026 09:30 | ⏳ Pending | ⚠️ Pending |

**Features:**
- Header row е **bold** и **gray background**
- Status колоната има **emoji икони**
- Completed rows - зелен цвят
- Pending rows - жълт цвят
- Auto-sized columns

#### **Sheet 2: Pending Parent Consents**
| Child Name | Email | Age | Parent Email | Agreed At | Token Expires | Days Left |
|------------|-------|-----|--------------|-----------|---------------|-----------|
| Петър Димитров | petar@example.com | 9 | dad@example.com | 04.03.2026 09:30 | 06.03.2026 09:30 | 1 |
| Анна Георгиева | anna@example.com | 8 | mom@example.com | 03.03.2026 15:00 | 05.03.2026 15:00 | 0 |

**Features:**
- **Days Left** - изчислява се с формула
- **Conditional formatting** - червен цвят при Days Left < 1

#### **Sheet 3: Agreement Versions**
| Version | Type | Effective Date | Total Users | Active |
|---------|------|----------------|-------------|--------|
| 2.0 | LIABILITY_WAIVER | 01.03.2026 | 64 | ✅ Yes |
| 1.0 | LIABILITY_WAIVER | 01.01.2024 | 78 | ❌ No |

#### **Sheet 4: Statistics**
```
Общо потребители:              150
Приети споразумения:           142
Pending parent consents:       8
Изтекли токени:                2

По възраст:
  Деца (< 18):                 45
  Възрастни (18+):             105

По версии:
  Version 1.0:                 78
  Version 2.0:                 64
  Version 2.1:                 8

По тип споразумение:
  LIABILITY_WAIVER:            142
  GDPR_CONSENT:                150
  TERMS_OF_SERVICE:            150

По метод на съгласие:
  DIRECT:                      105
  PARENT_EMAIL:                43
  ADMIN:                       2
```

---

### **3. PDF - Individual Certificate**

**Предимства:**
- ✅ Официален документ за печат
- ✅ Не може да се редактира (immutable)
- ✅ Универсално четим
- ✅ Може да се подпише дигитално

**Недостатъци:**
- ❌ Не може да се обработва в Excel
- ❌ Един потребител на файл

**Use case:**
- Официално потвърждение за потребителя
- Архив с физически документи
- Доказателство в съда

**Пример:** `user-agreement-certificate-Ivan-Petrov-2026-03-05.pdf`

```
┌─────────────────────────────────────────────────────────────┐
│                    DOJO CARD INDEX                          │
│              Потвърждение за съгласие                       │
│                                                              │
│              Освобождаване от отговорност                    │
└─────────────────────────────────────────────────────────────┘

═══════════════════════════════════════════════════════════════

ПОТРЕБИТЕЛ:

  Име:                  Иван Петров
  Email:                ivan@example.com
  Дата на раждане:      15.03.1995 (30 години)
  Потребител ID:        550e8400-e29b-41d4-a716-446655440000

═══════════════════════════════════════════════════════════════

СПОРАЗУМЕНИЕ:

  Тип:                  Освобождаване от отговорност
                        (LIABILITY_WAIVER)
  
  Версия:               2.0
  
  Дата на влизане       01.03.2026
  в сила:               

═══════════════════════════════════════════════════════════════

СЪГЛАСИЕ:

  Дата на приемане:     05.03.2026 14:23:45
  
  IP адрес:             192.168.1.100
  
  Метод:                Директно приемане (DIRECT)
  
  Статус:               ✅ Потвърдено

═══════════════════════════════════════════════════════════════

СЪДЪРЖАНИЕ НА СПОРАЗУМЕНИЕТО:

[Тук се вкарва пълния текст на Agreement.content - stripнат от HTML tags]

Аз, долуподписаният/долуподписаната, декларирам че съм запознат/а
с условията на настоящото споразумение и приемам всички клаузи...

═══════════════════════════════════════════════════════════════

ПРАВНА ИНФОРМАЦИЯ:

Този документ служи като доказателство за електронно дадено
съгласие съгласно:
  - Закон за електронния документ и електронните 
    удостоверителни услуги (ЗЕДЕУУ)
  - Регламент (ЕС) 2016/679 (GDPR)

Електронното съгласие има правна сила равностойна на 
хартиения документ.

─────────────────────────────────────────────────────────────

Генериран на:         05.03.2026 16:00:00
Администратор:        admin@dojo.com

┌─────────────────────────────────────────────────────────────┐
│  Dojo Card Index | www.dojocardindex.com | info@dojo.com   │
└─────────────────────────────────────────────────────────────┘
```

**За деца (с parent consent):**

```
═══════════════════════════════════════════════════════════════

РОДИТЕЛСКО СЪГЛАСИЕ:

  Потребителят е НЕПЪЛНОЛЕТЕН (< 18 години)
  
  Родител/Настойник:    parent@example.com
  
  Дата на               05.03.2026 18:45:22
  потвърждение:
  
  IP адрес на           89.26.45.123
  родител:
  
  Метод:                Email верификация (PARENT_EMAIL)
  
  Verification token:   880e8400-e29b-41d4-a716-446655440000
                        (validated ✅)
  
  Статус:               ✅ Потвърдено от родител

═══════════════════════════════════════════════════════════════
```

---

### **4. JSON - Structured Data**

**Предимства:**
- ✅ Програмно четим
- ✅ Пълна структура на данни
- ✅ Лесно за parse-ване
- ✅ Може да се import-не обратно

**Недостатъци:**
- ❌ Не е human-readable за нетехнически хора
- ❌ По-голям размер от CSV

**Use case:**
- Backup на база данни
- Import в друга система
- API integration
- Archive с metadata

**Файл:** `user-agreements-export-2026-03-05.json`

```json
{
  "exportMetadata": {
    "generatedAt": "2026-03-05T16:00:00",
    "generatedBy": "admin@dojo.com",
    "generatedByUserId": "admin-uuid-123",
    "exportVersion": "1.0",
    "totalRecords": 150,
    "filters": {
      "dateFrom": "2026-01-01",
      "dateTo": "2026-03-05",
      "agreementType": "LIABILITY_WAIVER",
      "onlyPending": false
    }
  },
  "agreements": [
    {
      "userId": "550e8400-e29b-41d4-a716-446655440000",
      "userEmail": "ivan@example.com",
      "firstName": "Иван",
      "lastName": "Петров",
      "birthDate": "1995-03-15",
      "age": 30,
      "agreement": {
        "id": "660f9500-e29b-41d4-a716-446655440000",
        "type": "LIABILITY_WAIVER",
        "version": "2.0",
        "effectiveDate": "2026-03-01",
        "createdAt": "2026-02-25T10:00:00",
        "contentHash": "sha256:a1b2c3d4e5f6...",
        "contentPreview": "Аз, долуподписаният, декларирам че..."
      },
      "consent": {
        "agreedAt": "2026-03-05T14:23:45",
        "ipAddress": "192.168.1.100",
        "consentMethod": "DIRECT",
        "isMinor": false,
        "status": "COMPLETED",
        "userAgreementId": "ua-550e8400-..."
      }
    },
    {
      "userId": "770e8400-e29b-41d4-a716-446655440001",
      "userEmail": "maria@example.com",
      "firstName": "Мария",
      "lastName": "Иванова",
      "birthDate": "2015-06-20",
      "age": 10,
      "agreement": {
        "id": "660f9500-e29b-41d4-a716-446655440000",
        "type": "LIABILITY_WAIVER",
        "version": "2.0",
        "effectiveDate": "2026-03-01",
        "createdAt": "2026-02-25T10:00:00",
        "contentHash": "sha256:a1b2c3d4e5f6..."
      },
      "consent": {
        "agreedAt": "2026-03-05T10:15:30",
        "ipAddress": "192.168.1.105",
        "consentMethod": "PARENT_EMAIL",
        "isMinor": true,
        "parentConsent": {
          "parentEmail": "parent@example.com",
          "parentConsentedAt": "2026-03-05T18:45:22",
          "parentIpAddress": "89.26.45.123",
          "parentConsentToken": "880e8400-e29b-41d4-a716-446655440002",
          "tokenExpiresAt": "2026-03-07T10:15:30",
          "tokenValid": true
        },
        "status": "COMPLETED",
        "userAgreementId": "ua-770e8400-..."
      }
    }
  ],
  "statistics": {
    "totalUsers": 150,
    "completedAgreements": 142,
    "pendingParentConsents": 8,
    "expiredTokens": 2,
    "byAge": {
      "minors": 45,
      "adults": 105
    },
    "byVersion": {
      "1.0": 78,
      "2.0": 64,
      "2.1": 8
    },
    "byAgreementType": {
      "LIABILITY_WAIVER": 142,
      "GDPR_CONSENT": 150,
      "TERMS_OF_SERVICE": 150
    },
    "byConsentMethod": {
      "DIRECT": 105,
      "PARENT_EMAIL": 43,
      "ADMIN": 2
    }
  }
}
```

---

## 🏗️ Backend архитектура

### **Package структура:**

```
cardindex.dojocardindex/
├── Agreement/
│   ├── models/
│   ├── repository/
│   └── service/
│       ├── AgreementService.java
│       └── AgreementExportService.java  ✨ NEW
│
├── UserAgreement/
│   ├── models/
│   ├── repository/
│   └── service/
│       └── UserAgreementService.java
│
└── export/  ✨ NEW PACKAGE
    ├── dto/
    │   ├── ExportFilter.java
    │   ├── ExportMetadata.java
    │   ├── ExportData.java
    │   └── AgreementStatistics.java
    ├── generator/
    │   ├── CsvExportGenerator.java
    │   ├── ExcelExportGenerator.java
    │   ├── PdfExportGenerator.java
    │   └── JsonExportGenerator.java
    └── specification/
        └── AgreementSpecifications.java
```

---

## 💻 Пълен код за имплементация

### **1. ExportFilter.java**

```java
package cardindex.dojocardindex.export.dto;

import cardindex.dojocardindex.Agreement.models.AgreementType;
import cardindex.dojocardindex.UserAgreement.models.ConsentMethod;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ExportFilter {
    
    private LocalDate dateFrom;
    private LocalDate dateTo;
    
    private AgreementType agreementType;
    private String version;
    
    private ConsentMethod consentMethod;
    
    private Boolean isMinor;
    private Boolean onlyPending;       // Само pending parent consents
    private Boolean onlyCompleted;     // Само fully agreed
    private Boolean includeExpired;    // Включи изтекли токени
    
    /**
     * Helper метод за валидация
     */
    public boolean hasDateFilter() {
        return dateFrom != null || dateTo != null;
    }
    
    /**
     * Helper метод за проверка дали има филтри
     */
    public boolean hasFilters() {
        return hasDateFilter() 
            || agreementType != null 
            || version != null
            || consentMethod != null
            || isMinor != null
            || onlyPending != null
            || onlyCompleted != null;
    }
}
```

### **2. AgreementStatistics.java**

```java
package cardindex.dojocardindex.export.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class AgreementStatistics {
    
    private int totalUsers;
    private int completedAgreements;
    private int pendingParentConsents;
    private int expiredTokens;
    
    private int minors;
    private int adults;
    
    private Map<String, Integer> byVersion;
    private Map<String, Integer> byAgreementType;
    private Map<String, Integer> byConsentMethod;
    
    /**
     * Процент на завършени споразумения
     */
    public double getCompletionRate() {
        if (totalUsers == 0) return 0.0;
        return (double) completedAgreements / totalUsers * 100;
    }
    
    /**
     * Процент на pending consents
     */
    public double getPendingRate() {
        if (totalUsers == 0) return 0.0;
        return (double) pendingParentConsents / totalUsers * 100;
    }
}
```

### **3. AgreementExportService.java**

```java
package cardindex.dojocardindex.Agreement.service;

import cardindex.dojocardindex.Agreement.models.AgreementType;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.repository.UserRepository;
import cardindex.dojocardindex.UserAgreement.models.UserAgreement;
import cardindex.dojocardindex.UserAgreement.repository.UserAgreementRepository;
import cardindex.dojocardindex.export.dto.AgreementStatistics;
import cardindex.dojocardindex.export.dto.ExportFilter;
import cardindex.dojocardindex.export.generator.*;
import cardindex.dojocardindex.export.specification.AgreementSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgreementExportService {
    
    private final UserAgreementRepository userAgreementRepository;
    private final UserRepository userRepository;
    private final CsvExportGenerator csvGenerator;
    private final ExcelExportGenerator excelGenerator;
    private final PdfExportGenerator pdfGenerator;
    private final JsonExportGenerator jsonGenerator;
    
    /**
     * Експорт на всички съгласия във CSV формат
     */
    public byte[] exportAllAgreementsAsCsv(ExportFilter filter) {
        log.info("Експорт на съгласия като CSV с филтри: {}", filter);
        List<UserAgreement> agreements = getFilteredAgreements(filter);
        return csvGenerator.generate(agreements);
    }
    
    /**
     * Експорт на всички съгласия в Excel формат
     */
    public byte[] exportAllAgreementsAsExcel(ExportFilter filter) {
        log.info("Експорт на съгласия като Excel с филтри: {}", filter);
        List<UserAgreement> agreements = getFilteredAgreements(filter);
        return excelGenerator.generate(agreements);
    }
    
    /**
     * Експорт на индивидуално потвърждение за потребител
     */
    public byte[] exportUserCertificatePdf(UUID userId, AgreementType type) {
        log.info("Генериране на PDF сертификат за user {} и тип {}", userId, type);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User не е намерен"));
        
        UserAgreement userAgreement = userAgreementRepository
            .findActiveUserAgreementByType(user, type)
            .orElseThrow(() -> new IllegalStateException(
                "User няма валидно споразумение от тип " + type));
        
        return pdfGenerator.generateCertificate(user, userAgreement);
    }
    
    /**
     * Експорт в JSON формат
     */
    public String exportAllAgreementsAsJson(ExportFilter filter) {
        log.info("Експорт на съгласия като JSON с филтри: {}", filter);
        List<UserAgreement> agreements = getFilteredAgreements(filter);
        return jsonGenerator.generate(agreements, filter);
    }
    
    /**
     * Експорт на pending parent consents
     */
    public byte[] exportPendingParentConsentsCsv() {
        log.info("Експорт на pending parent consents");
        List<UserAgreement> pending = userAgreementRepository.findPendingParentConsents();
        return csvGenerator.generatePendingConsents(pending);
    }
    
    /**
     * Генериране на статистики
     */
    public AgreementStatistics generateStatistics(ExportFilter filter) {
        List<UserAgreement> agreements = getFilteredAgreements(filter);
        return calculateStatistics(agreements);
    }
    
    /**
     * Филтриране на споразумения based on criteria
     */
    private List<UserAgreement> getFilteredAgreements(ExportFilter filter) {
        if (filter == null || !filter.hasFilters()) {
            return userAgreementRepository.findAll();
        }
        
        Specification<UserAgreement> spec = AgreementSpecifications.withFilter(filter);
        return userAgreementRepository.findAll(spec);
    }
    
    /**
     * Изчисляване на статистики
     */
    private AgreementStatistics calculateStatistics(List<UserAgreement> agreements) {
        Map<String, Integer> byVersion = agreements.stream()
            .collect(Collectors.groupingBy(
                ua -> ua.getAgreement().getVersion(),
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        
        Map<String, Integer> byType = agreements.stream()
            .collect(Collectors.groupingBy(
                ua -> ua.getAgreement().getType().toString(),
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        
        Map<String, Integer> byMethod = agreements.stream()
            .collect(Collectors.groupingBy(
                ua -> ua.getConsentMethod().toString(),
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));
        
        return AgreementStatistics.builder()
            .totalUsers(agreements.size())
            .completedAgreements((int) agreements.stream()
                .filter(UserAgreement::isFullyAgreed)
                .count())
            .pendingParentConsents((int) agreements.stream()
                .filter(UserAgreement::isPendingParentConsent)
                .count())
            .expiredTokens((int) agreements.stream()
                .filter(ua -> ua.isMinor() && !ua.isTokenValid())
                .count())
            .minors((int) agreements.stream()
                .filter(UserAgreement::isMinor)
                .count())
            .adults((int) agreements.stream()
                .filter(ua -> !ua.isMinor())
                .count())
            .byVersion(byVersion)
            .byAgreementType(byType)
            .byConsentMethod(byMethod)
            .build();
    }
}
```

### **4. CsvExportGenerator.java**

```java
package cardindex.dojocardindex.export.generator;

import cardindex.dojocardindex.Agreement.models.Agreement;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.UserAgreement.models.UserAgreement;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
public class CsvExportGenerator {
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd");
    
    /**
     * Генерира CSV за всички споразумения
     */
    public byte[] generate(List<UserAgreement> agreements) {
        log.info("Генериране на CSV за {} споразумения", agreements.size());
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8),
                CSVWriter.DEFAULT_SEPARATOR,
                CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                CSVWriter.DEFAULT_LINE_END)) {
            
            // BOM за правилно разпознаване на UTF-8 в Excel
            outputStream.write(0xEF);
            outputStream.write(0xBB);
            outputStream.write(0xBF);
            
            // Header
            writer.writeNext(new String[]{
                "User ID", "Email", "First Name", "Last Name", "Birth Date", "Age",
                "Agreement Type", "Agreement Version", "Agreed At", "IP Address",
                "Is Minor", "Parent Email", "Parent Consented At", "Parent IP",
                "Consent Method", "Status"
            });
            
            // Data rows
            for (UserAgreement ua : agreements) {
                writer.writeNext(toRow(ua));
            }
            
            log.info("CSV генериран успешно");
        } catch (Exception e) {
            log.error("Грешка при генериране на CSV", e);
            throw new RuntimeException("Грешка при генериране на CSV", e);
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Генерира CSV за pending parent consents
     */
    public byte[] generatePendingConsents(List<UserAgreement> pending) {
        log.info("Генериране на CSV за {} pending consents", pending.size());
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        try (CSVWriter writer = new CSVWriter(
                new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            
            // BOM
            outputStream.write(0xEF);
            outputStream.write(0xBB);
            outputStream.write(0xBF);
            
            // Header
            writer.writeNext(new String[]{
                "Child Name", "Child Email", "Age", "Parent Email", 
                "Agreed At", "Token Expires", "Days Left", "Status"
            });
            
            // Data rows
            for (UserAgreement ua : pending) {
                writer.writeNext(toPendingRow(ua));
            }
            
        } catch (Exception e) {
            log.error("Грешка при генериране на pending consents CSV", e);
            throw new RuntimeException("Грешка при генериране на pending consents CSV", e);
        }
        
        return outputStream.toByteArray();
    }
    
    /**
     * Конвертира UserAgreement в CSV row
     */
    private String[] toRow(UserAgreement ua) {
        User user = ua.getUser();
        Agreement agreement = ua.getAgreement();
        
        int age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();
        
        return new String[]{
            user.getId().toString(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getBirthDate().format(DATE_ONLY_FORMATTER),
            String.valueOf(age),
            agreement.getType().toString(),
            agreement.getVersion(),
            ua.getAgreedAt() != null ? ua.getAgreedAt().format(DATE_FORMATTER) : "",
            ua.getIpAddress() != null ? ua.getIpAddress() : "",
            String.valueOf(ua.isMinor()),
            ua.getParentEmail() != null ? ua.getParentEmail() : "",
            ua.getParentConsentedAt() != null ? 
                ua.getParentConsentedAt().format(DATE_FORMATTER) : "",
            ua.getParentIpAddress() != null ? ua.getParentIpAddress() : "",
            ua.getConsentMethod().toString(),
            getStatus(ua)
        };
    }
    
    /**
     * Конвертира pending consent в CSV row
     */
    private String[] toPendingRow(UserAgreement ua) {
        User user = ua.getUser();
        int age = Period.between(user.getBirthDate(), LocalDate.now()).getYears();
        
        long daysLeft = ua.getTokenExpiresAt() != null 
            ? Period.between(LocalDate.now(), 
                           ua.getTokenExpiresAt().toLocalDate()).getDays()
            : 0;
        
        return new String[]{
            user.getFirstName() + " " + user.getLastName(),
            user.getEmail(),
            String.valueOf(age),
            ua.getParentEmail(),
            ua.getAgreedAt().format(DATE_FORMATTER),
            ua.getTokenExpiresAt().format(DATE_FORMATTER),
            String.valueOf(daysLeft),
            daysLeft < 0 ? "Expired" : "Pending"
        };
    }
    
    /**
     * Определя статуса на споразумението
     */
    private String getStatus(UserAgreement ua) {
        if (ua.isFullyAgreed()) {
            return "Completed";
        }
        if (ua.isPendingParentConsent()) {
            return "Pending Parent";
        }
        if (ua.isMinor() && !ua.isTokenValid()) {
            return "Expired Token";
        }
        return "Incomplete";
    }
}
```

---

## 🌐 API документация

### **Base URL:** `/admin/export`

**Authentication:** Role ADMIN required

---

### **1. Export All Agreements as CSV**

```http
GET /admin/export/agreements/csv
```

**Query Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| dateFrom | LocalDate | No | От дата (YYYY-MM-DD) |
| dateTo | LocalDate | No | До дата (YYYY-MM-DD) |
| agreementType | AgreementType | No | LIABILITY_WAIVER / GDPR_CONSENT |
| version | String | No | Версия (1.0, 2.0) |
| isMinor | Boolean | No | true = само деца, false = само възрастни |
| onlyPending | Boolean | No | true = само pending parent consents |

**Response:**
```
Content-Type: text/csv
Content-Disposition: attachment; filename="user-agreements-export-2026-03-05.csv"

[CSV данни]
```

**Example:**
```bash
GET /admin/export/agreements/csv?dateFrom=2026-01-01&dateTo=2026-03-05&isMinor=true
```

---

### **2. Export All Agreements as Excel**

```http
GET /admin/export/agreements/excel
```

**Query Parameters:** (Same as CSV)

**Response:**
```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="user-agreements-export-2026-03-05.xlsx"

[Excel файл с 4 sheets]
```

---

### **3. Export All Agreements as JSON**

```http
GET /admin/export/agreements/json
```

**Query Parameters:** (Same as CSV)

**Response:**
```json
{
  "exportMetadata": {...},
  "agreements": [...],
  "statistics": {...}
}
```

---

### **4. Export Pending Parent Consents**

```http
GET /admin/export/pending-consents/csv
```

**Response:**
```
Content-Type: text/csv
Content-Disposition: attachment; filename="pending-parent-consents-2026-03-05.csv"
```

---

### **5. Export User Certificate (PDF)**

```http
GET /admin/export/user/{userId}/certificate/pdf
```

**Path Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| userId | UUID | Yes | User ID |

**Query Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| agreementType | AgreementType | No | LIABILITY_WAIVER | Тип на споразумението |

**Response:**
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="user-agreement-certificate-{userId}-2026-03-05.pdf"

[PDF файл]
```

**Example:**
```bash
GET /admin/export/user/550e8400-e29b-41d4-a716-446655440000/certificate/pdf?agreementType=LIABILITY_WAIVER
```

---

## 📦 Dependencies

Добави в `pom.xml`:

```xml
<!-- CSV export - OpenCSV -->
<dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>5.9</version>
</dependency>

<!-- Excel export - Apache POI -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- PDF export - iText 7 -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itext7-core</artifactId>
    <version>8.0.2</version>
    <type>pom</type>
</dependency>

<!-- JSON - Jackson (вероятно вече имаш) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
</dependency>
```

---

## ✅ Testing

### **Unit Tests:**

```java
@SpringBootTest
class AgreementExportServiceTest {
    
    @Mock
    private UserAgreementRepository userAgreementRepository;
    
    @Mock
    private CsvExportGenerator csvGenerator;
    
    @InjectMocks
    private AgreementExportService exportService;
    
    @Test
    void testExportCsvWithoutFilters() {
        // Given
        List<UserAgreement> agreements = createTestAgreements();
        when(userAgreementRepository.findAll()).thenReturn(agreements);
        when(csvGenerator.generate(agreements)).thenReturn("csv data".getBytes());
        
        // When
        byte[] result = exportService.exportAllAgreementsAsCsv(null);
        
        // Then
        assertNotNull(result);
        verify(csvGenerator).generate(agreements);
    }
    
    @Test
    void testExportCsvWithFilters() {
        // Test with date range filter
        ExportFilter filter = ExportFilter.builder()
            .dateFrom(LocalDate.of(2026, 1, 1))
            .dateTo(LocalDate.of(2026, 3, 5))
            .build();
        
        // ...
    }
}
```

---

## 📊 Summary

### **Експорт формати:**
1. ✅ **CSV** - за Excel обработка
2. ✅ **Excel** - с множество sheets и статистики
3. ✅ **PDF** - индивидуални сертификати
4. ✅ **JSON** - за програмна обработка

### **Филтри:**
- По период (dateFrom - dateTo)
- По тип споразумение
- По версия
- По статус (pending / completed)
- По възраст (деца / възрастни)

### **Use Cases:**
- Архив на клуба
- Audit trail
- Правна защита
- Статистически анализи
- Backup на данни

---

**Готово за имплементация!** 🚀

