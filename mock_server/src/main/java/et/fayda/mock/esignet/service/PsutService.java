package et.fayda.mock.esignet.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Generates the "sub" claim as a Partner Specific User Token (PSUT), mirroring the
 * algorithm found in MOSIP's own mock-identity-system:
 *
 *   sub = base64url( SHA3-256( individualId + relyingPartyId ) )
 *
 * Because relyingPartyId is part of the hash input, the same citizen gets a
 * DIFFERENT sub for every different relying party (pairwise, per OIDC's
 * subject_types_supported=pairwise) - but the SAME sub across multiple OIDC
 * clients that share one relyingPartyId registration.
 */
@Service
public class PsutService {

    private static final String PSUT_FORMAT = "%s%s"; // individualId + relyingPartyId

    public String generate(String individualId, String relyingPartyId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA3-256");
            byte[] hash = digest.digest(
                    PSUT_FORMAT.formatted(individualId, relyingPartyId).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate PSUT", e);
        }
    }
}
