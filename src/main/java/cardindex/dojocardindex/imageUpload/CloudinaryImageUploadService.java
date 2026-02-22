package cardindex.dojocardindex.imageUpload;

import cardindex.dojocardindex.exceptions.ImageDeleteException;
import cardindex.dojocardindex.exceptions.ImageUploadException;
import cardindex.dojocardindex.exceptions.InvalidImageFileException;
import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class CloudinaryImageUploadService implements ImageUploadService{

    private final Cloudinary cloudinary;

    public  CloudinaryImageUploadService(
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
        log.info("Cloudinary сървис инициализиран: {}", cloudName);
    }



    @Override
    public String uploadImage(MultipartFile file) {

        validateImage(file);

        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "dojo-profiles",
                            "resource_type", "image",
                            "unique_filename", true,  // Гарантира уникално име
                            "use_filename", false,     // Не използва оригиналното име
                            "transformation", new Transformation<>()
                                    .width(500)
                                    .height(500)
                                    .crop("fill")
                                    .gravity("face")
                                    .quality("auto"),
                            "format", "jpg"
                    ));
            String imageUrl = uploadResult.get("secure_url").toString();
            log.info("Изображението е качено: {}", imageUrl);
            return imageUrl;
        } catch (IOException e) {
            log.error("Грешка при качване на файла: {}", e.getMessage());

            // Опростена error message
            String errorMessage;
            if (e instanceof java.net.UnknownHostException ||
                e instanceof java.net.ConnectException) {
                errorMessage = "Няма връзка с интернет. Моля проверете връзката си.";
            } else {
                errorMessage = "Грешка при качване на снимката. Моля опитайте отново.";
            }

            throw new ImageUploadException(errorMessage, e);
        }

    }

    @Override
    public void deleteImage(String imageUrl){
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            String publicId = extractPublicIdFromUrl(imageUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("Изображение изтрито: {}", publicId);
            }
        } catch (Exception e) {
            log.error("Грешка при изтриване: {}", e.getMessage());
            throw new ImageDeleteException("Грешка при изтриване на изображението", e);
        }
    }

    private void validateImage(MultipartFile file){
        if(file == null || file.isEmpty()){
            throw new InvalidImageFileException("Заредете валиден файл!");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")){
            throw new InvalidImageFileException("Файлът трябва да бъде изображение");
        }

        // Проверка за размер
        long fileSize = file.getSize();
        long maxSize = 5 * 1024 * 1024; // 5MB

        if (fileSize > maxSize){
            throw new InvalidImageFileException("Размерът на файла не може да надвишава 5MB");
        }

        if (fileSize < 1024){ // 1KB минимум
            throw new InvalidImageFileException("Файлът е твърде малък");
        }
    }

    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            if (!imageUrl.contains("cloudinary.com")){
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
            log.error("Грешка при извличане на public_id",e);
            return null;
        }
    }

    /**
     * Проверява дали Cloudinary е достъпен (health check).
     * Използва се от мониторинг системи.
     *
     * @return true ако Cloudinary отговаря, false ако е паднал
     */
    public boolean isCloudinaryAvailable() {
        try {
            // Опитваме се да направим API заявка към Cloudinary
            // Използваме resources().rootFolders() което прави GET заявка
            cloudinary.api().rootFolders(ObjectUtils.emptyMap());
            log.debug("Cloudinary health check: UP");
            return true;
        } catch (Exception e) {
            log.warn("Cloudinary health check: DOWN - {}", e.getMessage());
            return false;
        }
    }
}
