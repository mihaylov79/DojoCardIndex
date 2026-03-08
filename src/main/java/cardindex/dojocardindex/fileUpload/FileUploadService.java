package cardindex.dojocardindex.fileUpload;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {


    String uploadFile(MultipartFile file);

    void deleteFile(String fileUrl);

    String replaceFile(String oldFileUrl, MultipartFile newFile);
}
