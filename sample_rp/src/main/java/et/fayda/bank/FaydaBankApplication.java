package et.fayda.bank;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Fayda OIDC banking demo.
 * Equivalent to Django's manage.py + wsgi.py — this boots the whole app.
 */
@SpringBootApplication
public class FaydaBankApplication {
    public static void main(String[] args) {
        SpringApplication.run(FaydaBankApplication.class, args);
    }
}
