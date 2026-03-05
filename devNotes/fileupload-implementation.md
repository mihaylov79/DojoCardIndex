# 📄 FileUpload Implementation за Cloudinary

## Обща информация
Този документ съдържа пълната имплементация на модул за upload на файлове (документи) чрез Cloudinary.
Модулът е **отделен** от съществуващия `ImageUploadService` за да спазва SOLID принципи.

---

## 📁 Структура на файловете

```
src/main/java/cardindex/dojocardindex/
│
├── fileUpload/                                    (NEW PACKAGE)
│   ├── FileUploadService.java                    (Interface)
│   ├── CloudinaryFileUploadService.java          (Cloudinary Implementation)
│   └── FileValidator.java                        (Helper за валидация)
│
├── Document/                                      (NEW PACKAGE)
│   ├── models/
│   │   ├── Document.java                         (Entity)
│   │   └── DocumentCategory.java                 (Enum)
│   ├── repository/
│   │   └── DocumentRepository.java               (JPA Repository)
│   └── service/
│       └── DocumentService.java                  (Business Logic)
│
├── web/
│   ├── DocumentController.java                   (MVC Controller)
│   └── dto/
│       └── CreateDocumentRequest.java            (DTO)
│
└── exceptions/
    ├── FileUploadException.java                  (NEW)
    ├── InvalidFileException.java                 (NEW)
    └── FileDeleteException.java                  (NEW)
```

---

## 🔧 1. FileUploadService Interface

**Path:** `src/main/java/cardindex/dojocardindex/fileUpload/FileUploadService.java`

```java
package cardindex.dojocardindex.fileUpload;

import org.springframework.web.multipart.MultipartFile;

/**
 * Интерфейс за upload на файлове (документи).
 * Позволява лесна замяна на storage provider (Cloudinary, S3, Google Drive и т.н.)
 */
public interface FileUploadService {

    /**
     * Upload на файл към external storage
     * 
     * @param file MultipartFile от формата
     * @return URL на качения файл
     * @throws InvalidFileException ако файлът не е валиден
     * @throws FileUploadException при грешка в upload процеса
     */
    String uploadFile(MultipartFile file);

    /**
     * Изтриване на файл от external storage
     * 
     * @param fileUrl Пълен URL на файла
     * @throws FileDeleteException при грешка в delete процеса
     */
    void deleteFile(String fileUrl);

    /**
     * Замяна на съществуващ файл с нов
     * 
     * @param oldFileUrl URL на стария файл
     * @param newFile Новият файл за upload
     * @return URL на новия файл
     */
    String replaceFile(String oldFileUrl, MultipartFile newFile);
}
```

---

## 🌥️ 2. CloudinaryFileUploadService Implementation

**Path:** `src/main/java/cardindex/dojocardindex/fileUpload/CloudinaryFileUploadService.java`

```java
package cardindex.dojocardindex.fileUpload;

import cardindex.dojocardindex.exceptions.FileDeleteException;
import cardindex.dojocardindex.exceptions.FileUploadException;
import cardindex.dojocardindex.exceptions.InvalidFileException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CloudinaryFileUploadService implements FileUploadService {

    private final Cloudinary cloudinary;
    
    // Допустими file extensions
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "pdf", "doc", "docx", "xls", "xlsx", "txt", "zip", "rar"
    );
    
    // Максимален размер: 50MB
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;
    
    // Минимален размер: 1KB
    private static final long MIN_FILE_SIZE = 1024;

    public CloudinaryFileUploadService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {

        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true,
                "timeout", 60000  // 60 секунди timeout
        ));
        log.info("Cloudinary File Upload сървис инициализиран: {}", cloudName);
    }

    @Override
    public String uploadFile(MultipartFile file) {
        validateFile(file);

        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                    "folder", "dojo-documents",
                    "resource_type", "raw",  // За не-изображения
                    "unique_filename", true,
                    "use_filename", true,    // Запазва оригиналното име
                    "access_mode", "public"
                )
            );

            String fileUrl = uploadResult.get("secure_url").toString();
            log.info("Файлът е качен успешно: {} ({})", 
                file.getOriginalFilename(), fileUrl);
            
            return fileUrl;

        } catch (IOException e) {
            log.error("Грешка при качване на файл: {}", e.getMessage());

            String errorMessage;
            if (e instanceof java.net.UnknownHostException ||
                e instanceof java.net.ConnectException) {
                errorMessage = "Няма връзка с интернет. Моля проверете връзката си.";
            } else {
                errorMessage = "Грешка при качване на файла. Моля опитайте отново.";
            }

            throw new FileUploadException(errorMessage, e);
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            log.warn("Опит за изтриване на празен URL");
            return;
        }

        String publicId = extractPublicIdFromUrl(fileUrl);
        
        if (publicId == null) {
            log.error("Не може да се извлече public_id от URL: {}", fileUrl);
            throw new FileDeleteException("Невалиден файлов URL");
        }

        try {
            Map<String, Object> deleteResult = cloudinary.uploader().destroy(
                publicId,
                ObjectUtils.asMap("resource_type", "raw")
            );

            String result = deleteResult.get("result").toString();
            
            if ("ok".equals(result)) {
                log.info("Файлът е изтрит успешно: {}", publicId);
            } else if ("not found".equals(result)) {
                log.warn("Файлът не е намерен в Cloudinary: {}", publicId);
            } else {
                log.error("Неочакван резултат при изтриване: {}", result);
                throw new FileDeleteException("Файлът не може да бъде изтрит");
            }

        } catch (IOException e) {
            log.error("Грешка при изтриване на файл: {}", e.getMessage());
            throw new FileDeleteException("Грешка при изтриване на файла", e);
        }
    }

    @Override
    public String replaceFile(String oldFileUrl, MultipartFile newFile) {
        // Upload новия файл
        String newFileUrl = uploadFile(newFile);
        
        // Изтриване на стария файл (ако upload е успешен)
        try {
            deleteFile(oldFileUrl);
        } catch (Exception e) {
            log.warn("Не може да изтрие стария файл: {}", oldFileUrl, e);
            // Не хвърляме exception - новият файл е вече качен
        }
        
        return newFileUrl;
    }

    /**
     * Валидация на файла преди upload
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Заредете валиден файл!");
        }

        // Проверка за размер
        long fileSize = file.getSize();
        
        if (fileSize > MAX_FILE_SIZE) {
            throw new InvalidFileException(
                String.format("Размерът на файла не може да надвишава %dMB", 
                    MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        if (fileSize < MIN_FILE_SIZE) {
            throw new InvalidFileException("Файлът е твърде малък");
        }

        // Проверка за разширение
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            throw new InvalidFileException("Файлът трябва да има разширение");
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException(
                "Неподдържан формат. Разрешени: " + String.join(", ", ALLOWED_EXTENSIONS)
            );
        }

        log.debug("Файл валидиран успешно: {} ({} bytes)", fileName, fileSize);
    }

    /**
     * Извлича public_id от Cloudinary URL
     */
    private String extractPublicIdFromUrl(String fileUrl) {
        try {
            if (!fileUrl.contains("cloudinary.com")) {
                return null;
            }

            String[] parts = fileUrl.split("/upload/");
            if (parts.length < 2) return null;

            String afterUpload = parts[1];
            String withoutVersion = afterUpload.replaceFirst("v\\d+/", "");

            // За raw files, не махаме extension-а
            return withoutVersion;
            
        } catch (Exception e) {
            log.error("Грешка при извличане на public_id", e);
            return null;
        }
    }

    /**
     * Health check за Cloudinary връзката
     */
    public boolean isCloudinaryAvailable() {
        try {
            cloudinary.api().ping();
            return true;
        } catch (Exception e) {
            log.error("Cloudinary е недостъпен: {}", e.getMessage());
            return false;
        }
    }
}
```

---

## 🛡️ 3. Custom Exceptions

### FileUploadException
**Path:** `src/main/java/cardindex/dojocardindex/exceptions/FileUploadException.java`

```java
package cardindex.dojocardindex.exceptions;

public class FileUploadException extends RuntimeException {
    
    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

### InvalidFileException
**Path:** `src/main/java/cardindex/dojocardindex/exceptions/InvalidFileException.java`

```java
package cardindex.dojocardindex.exceptions;

public class InvalidFileException extends RuntimeException {
    
    public InvalidFileException(String message) {
        super(message);
    }
}
```

### FileDeleteException
**Path:** `src/main/java/cardindex/dojocardindex/exceptions/FileDeleteException.java`

```java
package cardindex.dojocardindex.exceptions;

public class FileDeleteException extends RuntimeException {
    
    public FileDeleteException(String message) {
        super(message);
    }

    public FileDeleteException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

## 📦 4. Document Entity

**Path:** `src/main/java/cardindex/dojocardindex/Document/models/Document.java`

```java
package cardindex.dojocardindex.Document.models;

import cardindex.dojocardindex.User.models.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String fileUrl;

    @Column(nullable = false)
    private String fileName;

    @Column
    private String fileType;

    @Column
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedBy;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    @Column
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentCategory category;
}
```

---

## 🏷️ 5. DocumentCategory Enum

**Path:** `src/main/java/cardindex/dojocardindex/Document/models/DocumentCategory.java`

```java
package cardindex.dojocardindex.Document.models;

public enum DocumentCategory {
    USTAV("Устав"),
    PROTOCOL("Протокол"),
    RASPORED("Разписание"),
    REGULATION("Правилник"),
    REPORT("Отчет"),
    OTHER("Друго");

    private final String displayName;

    DocumentCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
```

---

## 🗄️ 6. DocumentRepository

**Path:** `src/main/java/cardindex/dojocardindex/Document/repository/DocumentRepository.java`

```java
package cardindex.dojocardindex.Document.repository;

import cardindex.dojocardindex.Document.models.Document;
import cardindex.dojocardindex.Document.models.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByActiveTrue();

    List<Document> findByActiveTrueAndCategory(DocumentCategory category);

    List<Document> findByActiveTrueOrderByUploadedAtDesc();
}
```

---

## 🧩 7. CreateDocumentRequest DTO

**Path:** `src/main/java/cardindex/dojocardindex/web/dto/CreateDocumentRequest.java`

```java
package cardindex.dojocardindex.web.dto;

import cardindex.dojocardindex.Document.models.DocumentCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDocumentRequest {

    @NotBlank(message = "Заглавието е задължително")
    @Size(max = 200, message = "Заглавието не може да надвишава 200 символа")
    private String title;

    @Size(max = 1000, message = "Описанието не може да надвишава 1000 символа")
    private String description;

    @NotNull(message = "Категорията е задължителна")
    private DocumentCategory category;
}
```

---

## 🎯 8. DocumentService

**Path:** `src/main/java/cardindex/dojocardindex/Document/service/DocumentService.java`

```java
package cardindex.dojocardindex.Document.service;

import cardindex.dojocardindex.Document.models.Document;
import cardindex.dojocardindex.Document.models.DocumentCategory;
import cardindex.dojocardindex.Document.repository.DocumentRepository;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.fileUpload.FileUploadService;
import cardindex.dojocardindex.web.dto.CreateDocumentRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class DocumentService {

    private final FileUploadService fileUploadService;
    private final DocumentRepository documentRepository;
    private final UserService userService;

    @Autowired
    public DocumentService(FileUploadService fileUploadService,
                          DocumentRepository documentRepository,
                          UserService userService) {
        this.fileUploadService = fileUploadService;
        this.documentRepository = documentRepository;
        this.userService = userService;
    }

    /**
     * Upload на нов документ
     */
    @Transactional
    public Document uploadDocument(CreateDocumentRequest request, 
                                   MultipartFile file) {
        
        User currentUser = userService.getCurrentUser();

        // Upload на файла
        String fileUrl = fileUploadService.uploadFile(file);

        // Създаване на документ entity
        Document document = Document.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedBy(currentUser)
                .uploadedAt(LocalDateTime.now())
                .category(request.getCategory())
                .active(true)
                .build();

        Document savedDocument = documentRepository.save(document);
        log.info("Документ качен успешно: {} от {}", 
            savedDocument.getTitle(), currentUser.getEmail());

        return savedDocument;
    }

    /**
     * Замяна на съществуващ документ
     */
    @Transactional
    public Document replaceDocument(UUID documentId, MultipartFile newFile) {
        Document document = getDocumentById(documentId);

        String oldFileUrl = document.getFileUrl();

        // Upload на новия файл и изтриване на стария
        String newFileUrl = fileUploadService.replaceFile(oldFileUrl, newFile);

        // Update на документа
        Document updatedDocument = document.toBuilder()
                .fileUrl(newFileUrl)
                .fileName(newFile.getOriginalFilename())
                .fileType(newFile.getContentType())
                .fileSize(newFile.getSize())
                .updatedAt(LocalDateTime.now())
                .build();

        return documentRepository.save(updatedDocument);
    }

    /**
     * Изтриване на документ (soft delete)
     */
    @Transactional
    public void deleteDocument(UUID documentId) {
        Document document = getDocumentById(documentId);

        // Soft delete
        Document deletedDocument = document.toBuilder()
                .active(false)
                .build();

        documentRepository.save(deletedDocument);

        // Опционално: изтриване на файла от Cloudinary
        try {
            fileUploadService.deleteFile(document.getFileUrl());
        } catch (Exception e) {
            log.warn("Не може да изтрие файла от Cloudinary: {}", 
                document.getFileUrl(), e);
        }

        log.info("Документ изтрит: {}", document.getTitle());
    }

    /**
     * Извличане на всички активни документи
     */
    public List<Document> getAllActiveDocuments() {
        return documentRepository.findByActiveTrueOrderByUploadedAtDesc();
    }

    /**
     * Извличане на документи по категория
     */
    public List<Document> getDocumentsByCategory(DocumentCategory category) {
        return documentRepository.findByActiveTrueAndCategory(category);
    }

    /**
     * Извличане на документ по ID
     */
    public Document getDocumentById(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new EntityNotFoundException(
                    "Документ с ID [%s] не е намерен".formatted(documentId)
                ));
    }
}
```

---

## 🌐 9. DocumentController

**Path:** `src/main/java/cardindex/dojocardindex/web/DocumentController.java`

```java
package cardindex.dojocardindex.web;

import cardindex.dojocardindex.Document.models.Document;
import cardindex.dojocardindex.Document.models.DocumentCategory;
import cardindex.dojocardindex.Document.service.DocumentService;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.security.CustomUserDetails;
import cardindex.dojocardindex.web.dto.CreateDocumentRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Slf4j
@Controller
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;
    private final UserService userService;

    @Autowired
    public DocumentController(DocumentService documentService, 
                             UserService userService) {
        this.documentService = documentService;
        this.userService = userService;
    }

    /**
     * Страница с всички документи
     */
    @GetMapping
    public ModelAndView getDocumentsPage(@AuthenticationPrincipal CustomUserDetails details) {
        
        User currentUser = userService.getUserById(details.getId());
        List<Document> documents = documentService.getAllActiveDocuments();

        ModelAndView modelAndView = new ModelAndView("documents");
        modelAndView.addObject("currentUser", currentUser);
        modelAndView.addObject("documents", documents);
        modelAndView.addObject("categories", DocumentCategory.values());

        return modelAndView;
    }

    /**
     * Форма за upload на документ
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @GetMapping("/upload")
    public ModelAndView getUploadPage(@AuthenticationPrincipal CustomUserDetails details) {
        
        User currentUser = userService.getUserById(details.getId());

        ModelAndView modelAndView = new ModelAndView("upload-document");
        modelAndView.addObject("currentUser", currentUser);
        modelAndView.addObject("createDocumentRequest", new CreateDocumentRequest());
        modelAndView.addObject("categories", DocumentCategory.values());

        return modelAndView;
    }

    /**
     * Upload на нов документ
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @PostMapping("/upload")
    public ModelAndView uploadDocument(
            @AuthenticationPrincipal CustomUserDetails details,
            @Valid @ModelAttribute CreateDocumentRequest createDocumentRequest,
            BindingResult result,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        User currentUser = userService.getUserById(details.getId());

        // Валидация на формата
        if (result.hasErrors()) {
            ModelAndView modelAndView = new ModelAndView("upload-document");
            modelAndView.addObject("currentUser", currentUser);
            modelAndView.addObject("categories", DocumentCategory.values());
            return modelAndView;
        }

        // Проверка дали има качен файл
        if (file.isEmpty()) {
            ModelAndView modelAndView = new ModelAndView("upload-document");
            modelAndView.addObject("currentUser", currentUser);
            modelAndView.addObject("categories", DocumentCategory.values());
            modelAndView.addObject("error", "Моля изберете файл за качване");
            return modelAndView;
        }

        try {
            documentService.uploadDocument(createDocumentRequest, file);
            redirectAttributes.addFlashAttribute("success", 
                "Документът е качен успешно!");
        } catch (Exception e) {
            log.error("Грешка при качване на документ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Грешка при качване: " + e.getMessage());
        }

        return new ModelAndView("redirect:/documents");
    }

    /**
     * Изтриване на документ
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/delete")
    public ModelAndView deleteDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal CustomUserDetails details,
            RedirectAttributes redirectAttributes) {

        try {
            documentService.deleteDocument(id);
            redirectAttributes.addFlashAttribute("success", 
                "Документът е изтрит успешно!");
        } catch (Exception e) {
            log.error("Грешка при изтриване на документ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Грешка при изтриване: " + e.getMessage());
        }

        return new ModelAndView("redirect:/documents");
    }

    /**
     * Замяна на документ
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'TRAINER')")
    @PostMapping("/{id}/replace")
    public ModelAndView replaceDocument(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails details,
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", 
                "Моля изберете файл за замяна");
            return new ModelAndView("redirect:/documents");
        }

        try {
            documentService.replaceDocument(id, file);
            redirectAttributes.addFlashAttribute("success", 
                "Документът е заменен успешно!");
        } catch (Exception e) {
            log.error("Грешка при замяна на документ", e);
            redirectAttributes.addFlashAttribute("error", 
                "Грешка при замяна: " + e.getMessage());
        }

        return new ModelAndView("redirect:/documents");
    }

    /**
     * Филтриране по категория
     */
    @GetMapping("/category/{category}")
    public ModelAndView getDocumentsByCategory(
            @PathVariable DocumentCategory category,
            @AuthenticationPrincipal CustomUserDetails details) {

        User currentUser = userService.getUserById(details.getId());
        List<Document> documents = documentService.getDocumentsByCategory(category);

        ModelAndView modelAndView = new ModelAndView("documents");
        modelAndView.addObject("currentUser", currentUser);
        modelAndView.addObject("documents", documents);
        modelAndView.addObject("categories", DocumentCategory.values());
        modelAndView.addObject("selectedCategory", category);

        return modelAndView;
    }
}
```

---

## 🗃️ 10. Database Migration (SQL)

```sql
-- Таблица за документи
CREATE TABLE documents (
    id BINARY(16) PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    file_url VARCHAR(500) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT,
    uploaded_by_user_id BINARY(16),
    uploaded_at DATETIME NOT NULL,
    updated_at DATETIME,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    category VARCHAR(50) NOT NULL,
    FOREIGN KEY (uploaded_by_user_id) REFERENCES users(id)
);

-- Индекси за по-бързи заявки
CREATE INDEX idx_documents_active ON documents(active);
CREATE INDEX idx_documents_category ON documents(category);
CREATE INDEX idx_documents_uploaded_at ON documents(uploaded_at DESC);
```

---

## 🎨 11. Thymeleaf Views (Примерен код)

### documents.html (Листване)
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <title>Документи на клуба</title>
</head>
<body>
    <h1>📄 Документи на клуба</h1>

    <!-- Бутон за upload (само за ADMIN/TRAINER) -->
    <div sec:authorize="hasAnyRole('ADMIN', 'TRAINER')">
        <a th:href="@{/documents/upload}" class="btn">➕ Качи документ</a>
    </div>

    <!-- Flash съобщения -->
    <div th:if="${success}" class="alert alert-success" th:text="${success}"></div>
    <div th:if="${error}" class="alert alert-danger" th:text="${error}"></div>

    <!-- Филтър по категория -->
    <div>
        <label>Категория:</label>
        <select onchange="location = this.value;">
            <option value="/documents">Всички</option>
            <option th:each="cat : ${categories}" 
                    th:value="@{/documents/category/{cat}(cat=${cat})}"
                    th:text="${cat.displayName}"
                    th:selected="${selectedCategory == cat}">
            </option>
        </select>
    </div>

    <!-- Списък с документи -->
    <div th:if="${documents.isEmpty()}">
        <p>Няма налични документи.</p>
    </div>

    <div th:each="doc : ${documents}" class="document-card">
        <h3 th:text="${doc.title}">Заглавие</h3>
        <p th:text="${doc.description}">Описание</p>
        
        <div class="metadata">
            <span>📁 <span th:text="${doc.category.displayName}"></span></span>
            <span>📅 <span th:text="${#temporals.format(doc.uploadedAt, 'dd.MM.yyyy HH:mm')}"></span></span>
            <span>👤 <span th:text="${doc.uploadedBy.firstName + ' ' + doc.uploadedBy.lastName}"></span></span>
            <span>💾 <span th:text="${doc.fileSize / 1024} + ' KB'"></span></span>
        </div>

        <!-- Действия -->
        <div class="actions">
            <a th:href="${doc.fileUrl}" target="_blank" class="btn">📥 Изтегли</a>

            <!-- Само за ADMIN/TRAINER -->
            <div sec:authorize="hasAnyRole('ADMIN', 'TRAINER')">
                <form th:action="@{/documents/{id}/replace(id=${doc.id})}" 
                      method="post" enctype="multipart/form-data" style="display:inline;">
                    <input type="file" name="file" required />
                    <button type="submit">🔄 Замени</button>
                </form>
            </div>

            <!-- Само за ADMIN -->
            <div sec:authorize="hasRole('ADMIN')">
                <form th:action="@{/documents/{id}/delete(id=${doc.id})}" 
                      method="post" style="display:inline;">
                    <button type="submit" onclick="return confirm('Сигурни ли сте?')">
                        🗑️ Изтрий
                    </button>
                </form>
            </div>
        </div>
    </div>
</body>
</html>
```

### upload-document.html (Форма)
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Качи документ</title>
</head>
<body>
    <h1>📤 Качи нов документ</h1>

    <form th:action="@{/documents/upload}" 
          method="post" 
          enctype="multipart/form-data"
          th:object="${createDocumentRequest}">

        <div>
            <label>Заглавие:</label>
            <input type="text" th:field="*{title}" required />
            <span th:if="${#fields.hasErrors('title')}" 
                  th:errors="*{title}" class="error"></span>
        </div>

        <div>
            <label>Описание:</label>
            <textarea th:field="*{description}" rows="4"></textarea>
            <span th:if="${#fields.hasErrors('description')}" 
                  th:errors="*{description}" class="error"></span>
        </div>

        <div>
            <label>Категория:</label>
            <select th:field="*{category}" required>
                <option value="">-- Изберете --</option>
                <option th:each="cat : ${categories}" 
                        th:value="${cat}" 
                        th:text="${cat.displayName}">
                </option>
            </select>
            <span th:if="${#fields.hasErrors('category')}" 
                  th:errors="*{category}" class="error"></span>
        </div>

        <div>
            <label>Файл:</label>
            <input type="file" name="file" 
                   accept=".pdf,.doc,.docx,.xls,.xlsx,.txt,.zip" 
                   required />
        </div>

        <div th:if="${error}" class="alert alert-danger" th:text="${error}"></div>

        <button type="submit">✅ Качи документ</button>
        <a th:href="@{/documents}">❌ Отказ</a>
    </form>
</body>
</html>
```

---

## ⚙️ 12. Конфигурация

### application.properties
```properties
# Cloudinary настройки (същите като за изображенията)
cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME}
cloudinary.api-key=${CLOUDINARY_API_KEY}
cloudinary.api-secret=${CLOUDINARY_API_SECRET}

# Upload размери
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

---

## ✅ Предимства на тази архитектура

1. **Separation of Concerns**
   - `ImageUploadService` → само за снимки
   - `FileUploadService` → само за документи

2. **Easy to Replace**
   - Искаш AWS S3? → Създай `S3FileUploadService implements FileUploadService`
   - Промяната е само в имплементацията, не в бизнес логиката

3. **Testable**
   - Mock-ваш лесно `FileUploadService` в unit тестовете
   - Не зависиш от external services

4. **Secure**
   - Валидация на файлове
   - Role-based access control
   - Soft delete за история

5. **Maintainable**
   - Ясна структура
   - Добра документация
   - Спазва SOLID принципи

---

## 🚀 Следващи стъпки

1. Създаване на package структурата
2. Копиране на класовете
3. DB migration (добавяне на таблицата)
4. Създаване на Thymeleaf views
5. Добавяне на линк в navigation menu
6. Тестване

---

## 📝 Забележки

- ⚠️ Cloudinary безплатният план поддържа до **25GB storage**
- ⚠️ За файлове използваме `resource_type: "raw"` (не "image")
- ⚠️ Препоръчително е virus scan за файловете (не е включено в тази версия)
- ✅ Soft delete позволява история и възстановяване

---

**Готово за имплементация! 🎉**

