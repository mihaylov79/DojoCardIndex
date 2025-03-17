package cardindex.dojocardindex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class DojoCardIndexApplication {

    public static void main(String[] args) {
        SpringApplication.run(DojoCardIndexApplication.class, args);
    }

}
