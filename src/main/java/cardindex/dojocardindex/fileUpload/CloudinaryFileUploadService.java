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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CloudinaryFileUploadService implements FileUploadService{

    private final Cloudinary cloudinary;

    private static final List<String> ALLOWED_FILES = Arrays.asList(
            "pdf", "xls", "xlsx", "doc", "docx", "zip"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
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
                "timeout", 60000
        ));
        log.info("Cloudinary file upload сървис инициализиран: {}", cloudName);
    }

    @Override
    public String uploadFile(MultipartFile file) {
        validateFile(file);

        try{
            Map<String,Object>uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                    "folder", "dojo-documents",
                    "resource_type", "raw",
                    "unique_filename", true,
                    "use-filename", true,
                    "access_mode", "public")
            );

            String fileUrl = uploadResult.get("secure_url").toString();
            log.info("Файлът е качен успешно: {} ({})",
                    file.getOriginalFilename(), fileUrl);

            return fileUrl;

        } catch (Exception e) {
            log.error("Грешка при качване на файл: {}", e.getMessage(), e);

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
        if (fileUrl == null || fileUrl.isEmpty()){
            log.warn("Опит за изтриване на несъществуваш или празен URL");
            return;
        }
        
        String publicId = extractPublicIdFromUrl(fileUrl);
        
        if (publicId == null){
            log.error("Не може да се извлече public_id от URL: {}", fileUrl);
            throw new FileDeleteException("Невалиден файлов URL");
        }
        
        try{
            Map<String, Object> deleteResult = cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", "raw")
            );
            
            String result = deleteResult.get("result").toString();
            
            switch (result) {

                case "ok" ->  log.info("Файлът е изтрит успешно: {}", publicId);
                case "not found" -> log.warn("Файлът не е намерен в Cloudinary: {}", publicId);
                default -> {
                    log.error("Неочакван резултат при изтриване: {}", result);
                throw new FileDeleteException("Файлът не може да бъде изтрит");
                }
                
            }
        } catch (Exception e) {
            log.error("Грешка при изтриване на файл: {}", e.getMessage());
            throw new FileDeleteException("Грешка при изтриване на файла", e);
        }

    }

    @Override
    public String replaceFile(String oldFileUrl, MultipartFile newFile) {
        
        String newFileUrl = uploadFile(newFile);
        
        try{
            deleteFile(oldFileUrl);
        } catch (Exception e) {
            log.warn("Не може да изтрие стария файл: {}", oldFileUrl, e);
        }
        return newFileUrl;
    }

    private void validateFile(MultipartFile file){
        if (file == null || file.isEmpty()){
            throw new InvalidFileException("Заредете валиден файл");
        }

        long fileSize = file.getSize();

        if (fileSize > MAX_FILE_SIZE || fileSize < MIN_FILE_SIZE) {
            throw new InvalidFileException(String.format(
                "Размерът на файла трябва да бъде между %d KB и %d MB.",
                MIN_FILE_SIZE / 1024,
                MAX_FILE_SIZE / (1024 * 1024)
            ));
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            throw new InvalidFileException("Файлът трябва да има разширение");
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_FILES.contains(extension)) {
            throw new InvalidFileException(String.format(
                "Неподдържан тип файл: %s. Разрешени типове: %s",
                extension,
                String.join(", ", ALLOWED_FILES)
            ));
        }
    }

    private String extractPublicIdFromUrl(String fileUrl) {
        try {
            if (!fileUrl.contains("cloudinary.com")) {
                return null;
            }

            String[] parts = fileUrl.split("/upload/");
            if (parts.length < 2) return null;

            String afterUpload = parts[1];

            // За raw files, не махаме extension-а
            return afterUpload.replaceFirst("v\\d+/", "");

        } catch (Exception e) {
            log.error("Грешка при извличане на public_id", e);
            return null;
        }
    }

    public boolean isCloudinaryAvailable() {
        try {
            cloudinary.api().ping(ObjectUtils.emptyMap());
            return true;
        } catch (Exception e) {
            log.error("Cloudinary е недостъпен: {}", e.getMessage());
            return false;
        }
    }

}
