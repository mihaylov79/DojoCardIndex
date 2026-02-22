package cardindex.dojocardindex.imageUpload;

import org.springframework.web.multipart.MultipartFile;


public interface ImageUploadService {

    String uploadImage(MultipartFile file);

    void deleteImage(String imageUrl);

}
