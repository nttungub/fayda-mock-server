package et.fayda.mock.esignet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FaydaMockEsignetApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaydaMockEsignetApplication.class, args);
        System.out.println("""

                Fayda Mock eSignet is running.
                  Discovery:  http://localhost:7088/.well-known/openid-configuration
                  Authorize:  http://localhost:7088/authorize
                  Token:      http://localhost:7088/oauth/v2/token
                  UserInfo:   http://localhost:7088/oidc/userinfo
                  JWKS:       http://localhost:7088/jwks.json

                See README.md for the full setup flow (register a client, create an identity, log in).
                """);
    }
}
