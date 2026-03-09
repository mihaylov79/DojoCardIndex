package cardindex.dojocardindex.Document.repository;

import cardindex.dojocardindex.Document.model.Document;
import cardindex.dojocardindex.Document.model.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository  extends JpaRepository<Document, UUID> {
    List<Document> findAllByActiveTrueOrderByUpdatedAtDesc();

    List<Document> findByActiveTrueAndCategoryOrderByUpdatedAtDesc(DocumentCategory category);

    List<Document> findByActiveTrueOrderByUploadedAtDesc();
}
