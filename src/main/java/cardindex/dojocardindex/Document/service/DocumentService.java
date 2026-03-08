package cardindex.dojocardindex.Document.service;


import cardindex.dojocardindex.Document.model.Document;
import cardindex.dojocardindex.Document.repository.DocumentRepository;
import cardindex.dojocardindex.User.models.User;
import cardindex.dojocardindex.User.service.UserService;
import cardindex.dojocardindex.exceptions.InvalidFileException;
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
    public DocumentService(FileUploadService fileUploadService, DocumentRepository documentRepository, UserService userService) {
        this.fileUploadService = fileUploadService;
        this.documentRepository = documentRepository;
        this.userService = userService;
    }


    public Document uploadDocument(CreateDocumentRequest request,
                                   MultipartFile file){

        if(file==null || file.isEmpty()){
            throw new InvalidFileException("Моля изберете файл за качване!");
        }

        if (file.getSize() == 0){
            throw new InvalidFileException("Файлът е празен");
        }

        User currentUser = userService.getCurrentUser();


        String fileUrl = fileUploadService.uploadFile(file);

        try {

            Document document = Document.builder()
                    .name(request.getTitle())
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

            log.info("{} беше качен успешно от {}",
                    savedDocument.getName(), currentUser.getEmail());

            return savedDocument;

        } catch (Exception e) {

            log.error("Грешка при запис в DB. Изтриване на файл от Cloudinary: {}", fileUrl);
            try {
                fileUploadService.deleteFile(fileUrl);
                log.info("Файлът е изтрит успешно от Cloudinary (cleanup)");
            } catch (Exception cleanupEx) {
                log.error("Неуспешен cleanup на файл от Cloudinary: {}", fileUrl, cleanupEx);
            }


            throw new RuntimeException("Грешка при качване на документ: " + e.getMessage(), e);
        }
    }

    public  Document replaceDocument(UUID documentId, MultipartFile newFile){
        Document document = getDocumentByID(documentId);

        String oldFileUrl =document.getFileUrl();

        String newFileUrl = fileUploadService.replaceFile(oldFileUrl, newFile);

        Document updateDocument = document.toBuilder()
                .fileUrl(newFileUrl)
                .fileName(newFile.getOriginalFilename())
                .fileType(newFile.getContentType())
                .fileSize(newFile.getSize())
                .updatedAt(LocalDateTime.now())
                .build();

        return documentRepository.save(updateDocument);
    }

    @Transactional
    public void deleteDocument(UUID documentId){

        Document document = getDocumentByID(documentId);

        Document softDeleted = document.toBuilder()
                .active(false)
                .build();
        documentRepository.save(softDeleted);

        try {
            fileUploadService.deleteFile(document.getFileUrl());

            documentRepository.delete(softDeleted);
            log.info("Документ [{}] изтрит напълно", document.getName());

        } catch (Exception e) {
            log.warn("Cloudinary изтриване неуспешно за [{}]. " +
                     "Документът остава като soft-deleted.",
                     document.getFileUrl(), e);

        }

    }

    public List<Document> getAllActiveDocuments(){
        return documentRepository.findAllByActiveTrueOrderByUpdatedAtDesc();
    }



    public Document getDocumentByID(UUID documentId){
        return documentRepository.findById(documentId).orElseThrow(() -> new EntityNotFoundException(
                "Документ с ID [%s] не е намерен".formatted(documentId)));
    }

}
