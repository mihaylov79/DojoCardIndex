package cardindex.dojocardindex.web.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ForgottenPasswordRequest {


    private String recipient;

    private String title;

    private String content;
}
