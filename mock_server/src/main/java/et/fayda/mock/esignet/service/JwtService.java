package et.fayda.mock.esignet.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;

@Service
public class JwtService {

    private final SigningKeyProvider keyProvider;

    public JwtService(SigningKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    /** Signs a claim set with this server's own RSA key (RS256). */
    public String sign(JWTClaimsSet claims) {
        try {
            RSAKey rsaKey = keyProvider.getSigningKey();
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(rsaKey.getKeyID())
                    .type(JOSEObjectType.JWT)
                    .build();
            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(new RSASSASigner(rsaKey));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    /**
     * Parses a client_assertion JWT and verifies its signature against the
     * relying party's registered public JWK (set via POST /admin/clients).
     * Throws if the signature is invalid, malformed, or expired.
     */
    public JWTClaimsSet verifyClientAssertion(String clientAssertion, String clientPublicJwkJson)
            throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(clientAssertion);
        RSAKey publicKey = RSAKey.parse(clientPublicJwkJson);
        RSASSAVerifier verifier = new RSASSAVerifier(publicKey.toRSAPublicKey());

        if (!signedJWT.verify(verifier)) {
            throw new IllegalArgumentException("client_assertion signature verification failed");
        }

        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        Date exp = claims.getExpirationTime();
        if (exp == null || exp.before(new Date())) {
            throw new IllegalArgumentException("client_assertion has expired or has no exp claim");
        }
        return claims;
    }
}
