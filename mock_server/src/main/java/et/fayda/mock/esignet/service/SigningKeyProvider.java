package et.fayda.mock.esignet.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Generates one RSA keypair per server run and uses it to sign id_token,
 * access_token, and userinfo JWTs. The public half is exposed at /jwks.json
 * so relying parties can verify signatures if they choose to.
 */
@Component
public class SigningKeyProvider {

    private final RSAKey rsaKey;

    public SigningKeyProvider() {
        try {
            this.rsaKey = new RSAKeyGenerator(2048)
                    .keyID(UUID.randomUUID().toString())
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .generate();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to generate mock signing key", e);
        }
    }

    public RSAKey getSigningKey() {
        return rsaKey;
    }

    public RSAKey getPublicJwk() {
        return rsaKey.toPublicJWK();
    }
}
