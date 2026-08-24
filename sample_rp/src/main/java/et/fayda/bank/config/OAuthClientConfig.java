package et.fayda.bank.config;

import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.NimbusJwtClientAuthenticationParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration
public class OAuthClientConfig {

    private final String privateKeyBase64;

    public OAuthClientConfig(@Value("${PRIVATE_KEY}") String privateKeyBase64) {
        this.privateKeyBase64 = privateKeyBase64;
    }

    private RSAKey loadRsaKey() {
        try {
            byte[] decoded = Base64.getDecoder().decode(privateKeyBase64.trim());
            String jwkJson = new String(decoded, StandardCharsets.UTF_8);
            RSAKey original = RSAKey.parse(jwkJson);

            // Rebuild the key WITHOUT the kid, so Nimbus does not add a 'kid'
            // to the JWT header — matching the working Django assertion, which
            // had only {alg, typ} in its header.
            return new RSAKey.Builder(original)
                    .keyID(null)
                    .build();

        } catch (Exception e) {
            throw new IllegalStateException("Failed to load Fayda RSA private key", e);
        }
    }

    @Bean
    public OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient() {
        RSAKey rsaKey = loadRsaKey();

        NimbusJwtClientAuthenticationParametersConverter<OAuth2AuthorizationCodeGrantRequest> jwtConverter =
                new NimbusJwtClientAuthenticationParametersConverter<>(
                        (ClientRegistration registration) -> rsaKey
                );

        OAuth2AuthorizationCodeGrantRequestEntityConverter requestEntityConverter =
                new OAuth2AuthorizationCodeGrantRequestEntityConverter();
        requestEntityConverter.addParametersConverter(jwtConverter);

        DefaultAuthorizationCodeTokenResponseClient tokenResponseClient =
                new DefaultAuthorizationCodeTokenResponseClient();
        tokenResponseClient.setRequestEntityConverter(requestEntityConverter);

        return tokenResponseClient;
    }
}