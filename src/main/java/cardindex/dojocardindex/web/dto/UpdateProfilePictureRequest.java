package cardindex.dojocardindex.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO за заявка за обновяване на профилна снимка.
 *
 * Използва се само за upload на нова снимка,
 * отделно от общото редактиране на профила.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfilePictureRequest {

    /**
     * Файлът с новата профилна снимка.
     */
    private MultipartFile image;
}

