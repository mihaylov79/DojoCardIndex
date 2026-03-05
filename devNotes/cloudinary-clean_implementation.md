# 🎯 ЧИСТА ИМПЛЕМЕНТАЦИЯ БЕЗ TRY-CATCH В CONTROLLER

---

## **1️⃣ Custom Exceptions**

### **InvalidImageFileException.java** (Валидационни грешки)

```java
package cardindex.dojocardindex.imageupload.exception;

/**
 * Хвърля се при невалиден image файл (празен, грешен формат, голям размер).
 */
public class InvalidImageFileException extends RuntimeException {
    
    public InvalidImageFileException(String message) {
        super(message);
    }
    
    public InvalidImageFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### **ImageUploadException.java** (Грешки при качване)

```java
package cardindex.dojocardindex.imageupload.exception;

/**
 * Хвърля се при грешка при качване на изображение в Cloudinary.
 */
public class ImageUploadException extends RuntimeException {
    
    public ImageUploadException(String message) {
        super(message);
    }
    
    public ImageUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### **ImageDeleteException.java** (Грешки при изтриване)

```java
package cardindex.dojocardindex.imageupload.exception;

/**
 * Хвърля се при грешка при изтриване на изображение от Cloudinary.
 */
public class ImageDeleteException extends RuntimeException {
    
    public ImageDeleteException(String message) {
        super(message);
    }
    
    public ImageDeleteException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

## **2️⃣ ImageUploadService.java** (Interface)

```java
package cardindex.dojocardindex.imageupload.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageUploadService {
    
    /**
     * Качва изображение в Cloudinary.
     * 
     * @param file MultipartFile с изображението
     * @return URL на качената снимка
     * @throws InvalidImageFileException ако файлът е невалиден
     * @throws ImageUploadException ако качването се провали
     */
    String uploadImage(MultipartFile file);
    
    /**
     * Изтрива изображение от Cloudinary.
     * 
     * @param imageUrl URL на изображението за изтриване
     * @throws ImageDeleteException ако изтриването се провали
     */
    void deleteImage(String imageUrl);
}
```

---

## **3️⃣ CloudinaryImageService.java** (Implementation)

```java
package cardindex.dojocardindex.imageupload.service;

import cardindex.dojocardindex.imageupload.exception.ImageDeleteException;
import cardindex.dojocardindex.imageupload.exception.ImageUploadException;
import cardindex.dojocardindex.imageupload.exception.InvalidImageFileException;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryImageService implements ImageUploadService {

    private static final Logger log = LoggerFactory.getLogger(CloudinaryImageService.class);
    private final Cloudinary cloudinary;

    public CloudinaryImageService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {
        
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
        
        log.info("Cloudinary сервис инициализиран: {}", cloudName);
    }

    @Override
    public String uploadImage(MultipartFile file) {
        
        validateImageFile(file);

        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "dojo-profiles",
                            "resource_type", "image",
                            "transformation", new Transformation<>()
                                    .width(500)
                                    .height(500)
                                    .crop("fill")
                                    .gravity("face")
                                    .quality("auto"),
                            "format", "jpg"
                    ));

            String imageUrl = uploadResult.get("secure_url").toString();
            log.info("Изображение качено: {}", imageUrl);
            return imageUrl;
            
        } catch (IOException e) {
            log.error("Грешка при качване в Cloudinary", e);
            throw new ImageUploadException("Грешка при качване: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            String publicId = extractPublicIdFromUrl(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Изображение изтрито: {}", publicId);
            }
        } catch (IOException e) {
            log.error("Грешка при изтриване от Cloudinary", e);
            throw new ImageDeleteException("Грешка при изтриване: " + e.getMessage(), e);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageFileException("Файлът не може да бъде празен");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new InvalidImageFileException("Файлът трябва да бъде изображение");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new InvalidImageFileException("Размерът не трябва да надвишава 5MB");
        }
    }

    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            if (!imageUrl.contains("cloudinary.com")) {
                return null;
            }
            
            String[] parts = imageUrl.split("/upload/");
            if (parts.length < 2) return null;
            
            String afterUpload = parts[1];
            String withoutVersion = afterUpload.replaceFirst("v\\d+/", "");
            
            int lastDot = withoutVersion.lastIndexOf('.');
            if (lastDot > 0) {
                withoutVersion = withoutVersion.substring(0, lastDot);
            }
            
            return withoutVersion;
        } catch (Exception e) {
            log.error("Грешка при извличане на public_id", e);
            return null;
        }
    }
}
```

---

## **4️⃣ UserService.java** - Нов метод (БЕЗ try-catch)

```java
// В UserService.java добави:

import cardindex.dojocardindex.imageupload.exception.ImageDeleteException;
import cardindex.dojocardindex.imageupload.exception.ImageUploadException;
import cardindex.dojocardindex.imageupload.exception.InvalidImageFileException;
import cardindex.dojocardindex.imageupload.service.ImageUploadService;
import org.springframework.web.multipart.MultipartFile;

private final ImageUploadService imageUploadService;

// Обнови конструктора:
@Autowired
public UserService(UserRepository userRepository, 
                   PasswordEncoder passwordEncoder, 
                   NotificationService notificationService,
                   ImageUploadService imageUploadService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.notificationService = notificationService;
    this.imageUploadService = imageUploadService;
}

/**
 * Обновява профилната снимка на потребителя.
 * 
 * @param userId ID на потребителя
 * @param image Новата снимка
 * @throws InvalidImageFileException ако файлът е невалиден
 * @throws ImageUploadException ако качването се провали
 * @throws ImageDeleteException ако изтриването на старата снимка се провали
 */
public void updateProfilePicture(UUID userId, MultipartFile image) {
    
    if (image == null || image.isEmpty()) {
        throw new InvalidImageFileException("Изображението не може да бъде празно");
    }
    
    log.info("Започва обновяване на профилна снимка за потребител: {}", userId);
    
    User user = getUserById(userId);
    String oldProfilePicture = user.getProfilePicture();
    
    // Качваме новата снимка ПЪРВО (ако се провали, не губим старата)
    String newImageUrl = imageUploadService.uploadImage(image);
    log.info("Нова снимка качена: {}", newImageUrl);
    
    // Обновяваме базата данни
    user = user.toBuilder()
            .profilePicture(newImageUrl)
            .build();
    userRepository.save(user);
    log.info("Профилна снимка обновена в базата данни за потребител: {}", userId);
    
    // Изтриваме старата снимка СЛЕД като сме сигурни че новата е запазена
    if (oldProfilePicture != null && !oldProfilePicture.isEmpty()) {
        try {
            imageUploadService.deleteImage(oldProfilePicture);
            log.info("Стара снимка изтрита: {}", oldProfilePicture);
        } catch (ImageDeleteException e) {
            // Логваме грешката, но не спираме процеса
            // Новата снимка вече е качена и базата е обновена
            log.warn("Не успяхме да изтрием старата снимка: {}", e.getMessage());
        }
    }
    
    log.info("Профилна снимка обновена успешно за потребител: {}", userId);
}
```

---

## **5️⃣ UserController.java** - Чист контролер (БЕЗ try-catch)

```java
// В UserController.java добави:

import cardindex.dojocardindex.imageupload.exception.InvalidImageFileException;
import cardindex.dojocardindex.imageupload.exception.ImageUploadException;
import cardindex.dojocardindex.imageupload.exception.ImageDeleteException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ...existing code...

    /**
     * Endpoint за качване на профилна снимка.
     * Делегира логиката на UserService.
     * Exceptions се обработват от GlobalExceptionHandler.
     */
    @PostMapping("/users/upload-profile-picture")
    public ModelAndView uploadProfilePicture(
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails details,
            RedirectAttributes redirectAttributes) {
        
        log.info("Заявка за качване на снимка от потребител: {}", details.getId());
        
        // Просто извикваме service метода - ако има exception, GlobalExceptionHandler го прихваща
        userService.updateProfilePicture(details.getId(), image);
        
        redirectAttributes.addFlashAttribute("successMessage", 
                "Профилната снимка беше обновена успешно!");
        
        return new ModelAndView("redirect:/users/details/" + details.getId());
    }

    // ...existing code...
}
```

---

## **6️⃣ GlobalExceptionHandler.java** - @ControllerAdvice

```java
package cardindex.dojocardindex.web.exception;

import cardindex.dojocardindex.imageupload.exception.InvalidImageFileException;
import cardindex.dojocardindex.imageupload.exception.ImageUploadException;
import cardindex.dojocardindex.imageupload.exception.ImageDeleteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Глобален обработчик на exceptions за целия application.
 * Прихваща exceptions от всички @Controller класове.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Обработва InvalidImageFileException (невалиден файл)
     */
    @ExceptionHandler(InvalidImageFileException.class)
    public ModelAndView handleInvalidImageFileException(
            InvalidImageFileException e, 
            RedirectAttributes redirectAttributes) {
        
        log.warn("Невалиден image файл: {}", e.getMessage());
        
        redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        return new ModelAndView("redirect:/home");
    }

    /**
     * Обработва ImageUploadException (грешка при качване)
     */
    @ExceptionHandler(ImageUploadException.class)
    public ModelAndView handleImageUploadException(
            ImageUploadException e, 
            RedirectAttributes redirectAttributes) {
        
        log.error("Грешка при качване на изображение", e);
        
        redirectAttributes.addFlashAttribute("errorMessage", 
                "Възникна грешка при качване на снимката. Моля опитайте отново.");
        return new ModelAndView("redirect:/home");
    }

    /**
     * Обработва ImageDeleteException (грешка при изтриване)
     */
    @ExceptionHandler(ImageDeleteException.class)
    public ModelAndView handleImageDeleteException(
            ImageDeleteException e, 
            RedirectAttributes redirectAttributes) {
        
        log.error("Грешка при изтриване на изображение", e);
        
        redirectAttributes.addFlashAttribute("errorMessage", 
                "Възникна грешка при изтриване на старата снимка. Моля опитайте отново.");
        return new ModelAndView("redirect:/home");
    }

    /**
     * Обработва MaxUploadSizeExceededException (файлът е твърде голям)
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ModelAndView handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e, 
            RedirectAttributes redirectAttributes) {
        
        log.warn("Файлът е твърде голям: {}", e.getMessage());
        
        redirectAttributes.addFlashAttribute("errorMessage", 
                "Файлът е твърде голям. Максималният размер е 5MB.");
        return new ModelAndView("redirect:/home");
    }

    /**
     * Обработва всички останали неочаквани exceptions
     */
    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(
            Exception e, 
            RedirectAttributes redirectAttributes) {
        
        log.error("Неочаквана грешка", e);
        
        redirectAttributes.addFlashAttribute("errorMessage", 
                "Възникна неочаквана грешка. Моля свържете се с администратор.");
        return new ModelAndView("redirect:/error");
    }
}
```

---

## **7️⃣ application.properties**

```properties
# Cloudinary Configuration
cloudinary.cloud-name=your-cloud-name
cloudinary.api-key=your-api-key
cloudinary.api-secret=your-api-secret

# File Upload Configuration
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
```

### **Алтернатива: Production конфигурация с environment variables**

```properties
# application.properties
cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:default-cloud-name}
cloudinary.api-key=${CLOUDINARY_API_KEY:default-api-key}
cloudinary.api-secret=${CLOUDINARY_API_SECRET:default-api-secret}

spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB
```

Създай `.env` файл в root директорията (добави в `.gitignore`):
```env
CLOUDINARY_CLOUD_NAME=actual-cloud-name
CLOUDINARY_API_KEY=actual-api-key
CLOUDINARY_API_SECRET=actual-api-secret
```

---

## **8️⃣ pom.xml - Dependencies**

### **Минимална конфигурация (препоръчвана):**

```xml
<dependencies>
    <!-- Existing dependencies... -->

    <!-- Cloudinary SDK (HTTP 5 - за Java 11+) -->
    <dependency>
        <groupId>com.cloudinary</groupId>
        <artifactId>cloudinary-http5</artifactId>
        <version>2.3.2</version>
    </dependency>
</dependencies>
```

### **Пълна конфигурация (с environment variables support):**

```xml
<dependencies>
    <!-- Existing dependencies... -->

    <!-- Cloudinary SDK (HTTP 5 - за Java 11+) -->
    <dependency>
        <groupId>com.cloudinary</groupId>
        <artifactId>cloudinary-http5</artifactId>
        <version>2.3.2</version>
    </dependency>

    <!-- dotenv за зареждане на .env файлове (опционално, за production) -->
    <dependency>
        <groupId>io.github.cdimascio</groupId>
        <artifactId>dotenv-java</artifactId>
        <version>2.2.4</version>
    </dependency>
</dependencies>
```

### **📝 Бележки за dependencies:**

| Dependency | Версия | Описание | Задължителна? |
|------------|--------|----------|---------------|
| **cloudinary-http5** | **2.3.2** | Cloudinary SDK с Apache HttpClient 5.x (за Java 11+) | ✅ **ДА** |
| **dotenv-java** | 2.2.4 | Зарежда environment variables от .env файлове | 🟡 Опционално |

**Защо `cloudinary-http5` v2.3.2?**
- ✅ **Най-новата stable версия** (Ноември 2024)
- ✅ Твоят проект използва Java 17 (Spring Boot 3.4.2 изисква Java 17+)
- ✅ HTTP Client 5 с обновени security patches и bug fixes
- ✅ Официалната препоръка на Cloudinary за нови проекти
- ✅ Оптимизирана за производителност и стабилност

**Алтернативи (не се препоръчват за твоя проект):**
- `cloudinary-http44` - за стари проекти с Java 8 и HttpClient 4.4
- `cloudinary-http45` - за проекти с Java 8-11 и HttpClient 4.5
- `cloudinary-taglib` - само за JSP проекти (ти използваш Thymeleaf)

---

## **9️⃣ HTML Форма**

### **📁 Структура на imageupload package:**

```
imageupload/
├── exception/
│   ├── InvalidImageFileException.java
│   ├── ImageUploadException.java
│   └── ImageDeleteException.java
├── service/
│   ├── ImageUploadService.java (interface)
│   └── CloudinaryImageService.java (implementation)
```

---

### **HTML темплейт за качване на снимка:**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Редактиране на профил</title>
</head>
<body>

<!-- Success/Error съобщения -->
<div th:if="${successMessage}" class="alert alert-success">
    <span th:text="${successMessage}"></span>
</div>

<div th:if="${errorMessage}" class="alert alert-danger">
    <span th:text="${errorMessage}"></span>
</div>

<!-- Секция с профилна снимка -->
<div class="profile-picture-section">
    <h3>Профилна снимка</h3>
    
    <!-- Текуща снимка -->
    <div th:if="${user.profilePicture != null && !user.profilePicture.isEmpty()}">
        <img th:src="${user.profilePicture}" 
             alt="Профилна снимка" 
             style="width: 200px; height: 200px; border-radius: 50%; object-fit: cover; border: 3px solid #ddd;">
    </div>
    
    <!-- Placeholder ако няма снимка -->
    <div th:unless="${user.profilePicture != null && !user.profilePicture.isEmpty()}">
        <img src="/images/default-avatar.png" 
             alt="Няма снимка" 
             style="width: 200px; height: 200px; border-radius: 50%; object-fit: cover; border: 3px solid #ddd;">
    </div>
    
    <!-- Форма за качване на нова снимка -->
    <form th:action="@{/users/upload-profile-picture}" 
          method="post" 
          enctype="multipart/form-data"
          class="mt-3">
        
        <div class="form-group">
            <label for="image">Качи нова профилна снимка:</label>
            <input type="file" 
                   id="image" 
                   name="image" 
                   accept="image/*" 
                   required 
                   class="form-control">
            <small class="form-text text-muted">
                Поддържани формати: JPG, PNG, GIF. Максимален размер: 5MB.
                Снимката автоматично ще бъде оптимизирана до 500x500px.
            </small>
        </div>
        
        <button type="submit" class="btn btn-primary">
            <i class="fas fa-upload"></i> Качи снимка
        </button>
    </form>
</div>

<hr>

<!-- Форма за редактиране на останалите данни -->
<form th:action="@{/users/details/edit/{id}(id=${user.id})}" 
      th:object="${editUserProfileRequest}" 
      method="post">
    
    <div class="form-group">
        <label for="firstName">Име:</label>
        <input type="text" 
               id="firstName" 
               th:field="*{firstName}" 
               class="form-control">
    </div>
    
    <div class="form-group">
        <label for="lastName">Фамилия:</label>
        <input type="text" 
               id="lastName" 
               th:field="*{lastName}" 
               class="form-control">
    </div>
    
    <!-- Други полета... -->
    
    <!-- ЗАБЕЛЕЖКА: Премахваме поле за profilePicture -->
    <!-- profilePicture се променя САМО чрез file upload формата -->
    
    <button type="submit" class="btn btn-success">Запази промени</button>
</form>

</body>
</html>
```

---

## ✅ **ПРЕДИМСТВА НА ТОЗИ ПОДХОД**

### **1. Чист Controller:**
```java
@PostMapping("/users/upload-profile-picture")
public ModelAndView uploadProfilePicture(...) {
    userService.updateProfilePicture(userId, image);  // Просто извикване
    return new ModelAndView("redirect:...");
}
```
- ✅ Няма try-catch блокове
- ✅ Няма бизнес логика
- ✅ Просто делегира на Service

### **2. Service хвърля специфични exceptions:**
```java
public void updateProfilePicture(UUID userId, MultipartFile image) {
    // Ако нещо се провали, хвърля специфичен exception
    String url = imageUploadService.uploadImage(image);  // throws ImageUploadException
    // ...
}
```
- ✅ Не се занимава с error handling
- ✅ Фокусира се върху бизнес логиката
- ✅ Single Responsibility Principle
- ✅ Хвърля конкретни Custom Exception-и

### **3. Custom Exceptions осигуряват прецизност:**
```java
// Вместо общи IllegalArgumentException и IOException:
throw new InvalidImageFileException("Невалиден формат");    // Валидация
throw new ImageUploadException("Грешка при качване");       // Upload проблем
throw new ImageDeleteException("Грешка при изтриване");     // Delete проблем
```
- ✅ **Специфичност** - ясно се вижда какъв е проблемът
- ✅ **Изолация** - не се бърка с други грешки в приложението
- ✅ **По-добър debugging** - stack trace показва конкретния проблем
- ✅ **Различна обработка** - всяка грешка може да се обработи по различен начин

### **4. @ControllerAdvice ги прихваща централизирано:**
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(InvalidImageFileException.class)
    public ModelAndView handleInvalidImageFileException(...) {
        // Специфична обработка за валидационни грешки
    }
    
    @ExceptionHandler(ImageUploadException.class)
    public ModelAndView handleImageUploadException(...) {
        // Специфична обработка за upload грешки
    }
    
    @ExceptionHandler(ImageDeleteException.class)
    public ModelAndView handleImageDeleteException(...) {
        // Специфична обработка за delete грешки
    }
}
```
- ✅ Централизирано error handling
- ✅ Една точка за всички грешки
- ✅ Лесно за поддръжка
- ✅ DRY (Don't Repeat Yourself)
- ✅ Различни съобщения и redirect-и за различни грешки

---

## 🎯 **ПОТОК НА ДАННИТЕ**

```
1. User избира снимка от HTML форма
   ↓
2. POST /users/upload-profile-picture
   UserController.uploadProfilePicture()
   ↓
3. userService.updateProfilePicture(userId, image)
   - Валидира image
   - Вземa User от базата
   - Изтрива старата снимка (ако има)
   - Качва новата снимка
   ↓
4. imageUploadService.uploadImage(image)
   - Валидира файла (тип, размер)
   - Конвертира в bytes
   - Праща към Cloudinary API
   ↓
5. Cloudinary обработва:
   - Resize на 500x500px
   - Crop с фокус към лицето (AI)
   - Оптимизация на качеството
   - Конвертиране в JPG
   - Съхранява в папка "dojo-profiles"
   ↓
6. Cloudinary връща secure_url
   ↓
7. UserService обновява user.profilePicture в MySQL
   ↓
8. Controller прави redirect към /users/details/{id}
   ↓
9. Thymeleaf показва <img th:src="${user.profilePicture}">
   ↓
10. Browser зарежда снимката от Cloudinary CDN
```

---

## 🚀 **СТЪПКИ ЗА ИМПЛЕМЕНТАЦИЯ**

### **Предварителна подготовка:**
1. ✅ Регистрирай се в https://cloudinary.com
2. ✅ Копирай Cloud Name, API Key, API Secret от Dashboard
3. ✅ Добави в `.gitignore`: `.env` (ако ползваш dotenv)

### **Имплементация:**
1. ✅ Обнови `pom.xml` с `cloudinary-http5` dependency
2. ✅ Добави credentials в `application.properties`
3. ✅ Създай `ImageUploadException.java`
4. ✅ Създай `ImageUploadService.java` (interface)
5. ✅ Създай `CloudinaryImageService.java` (implementation)
6. ✅ Създай `GlobalExceptionHandler.java` (@ControllerAdvice)
7. ✅ Обнови `UserService.java` (добави dependency + метод)
8. ✅ Обнови `UserController.java` (добави endpoint)
9. ✅ Обнови HTML темплейта (добави форма за upload)
10. ✅ Тествай функционалността

---

## 📊 **СРАВНЕНИЕ: Преди vs След**

| Аспект | Преди | След |
|--------|-------|------|
| **Съхранение** | На сървъра (файлова система) | Cloudinary CDN |
| **Размер** | Различни размери | Унифициран 500x500px |
| **Оптимизация** | Ръчна | Автоматична (AI) |
| **URL** | `/uploads/user123.jpg` | `https://res.cloudinary.com/...` |
| **Зареждане** | От твоя сървър | От CDN (по-бързо) |
| **Backup** | Трябва да се прави ръчно | Автоматично в cloud |
| **Scaling** | Проблем при много снимки | Безкрайно скалиране |

---

**Готово! Документацията е обновена с правилните dependencies (`cloudinary-http5` + опционално `dotenv-java`). Искаш ли да започнем имплементацията?** 🚀
