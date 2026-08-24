package et.fayda.mock.esignet.controller;

import com.nimbusds.jwt.JWTClaimsSet;
import et.fayda.mock.esignet.model.AuthTransaction;
import et.fayda.mock.esignet.model.OidcClient;
import et.fayda.mock.esignet.service.JwtService;
import et.fayda.mock.esignet.service.PsutService;
import et.fayda.mock.esignet.store.DataStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class TokenController {

    // Keep in sync with server.port in application.yml and BASE_URL in DiscoveryController.
    private static final String ISSUER = "http://localhost:7088";
    private static final String TOKEN_ENDPOINT = ISSUER + "/oauth/v2/token";
    private static final int ACCESS_TOKEN_TTL_SECONDS = 3600;
    private static final int ID_TOKEN_TTL_SECONDS = 3600;

    private final DataStore store;
    private final JwtService jwtService;
    private final PsutService psutService;

    public TokenController(DataStore store, JwtService jwtService, PsutService psutService) {
        this.store = store;
        this.jwtService = jwtService;
        this.psutService = psutService;
    }

    @PostMapping(value = "/oauth/v2/token", consumes = "application/x-www-form-urlencoded")
    public ResponseEntity<Map<String, Object>> token(@RequestParam String grant_type,
                                                      @RequestParam String code,
                                                      @RequestParam String redirect_uri,
                                                      @RequestParam String client_id,
                                                      @RequestParam(required = false) String client_assertion,
                                                      @RequestParam(required = false) String client_assertion_type,
                                                      @RequestParam(required = false) String code_verifier) {

        if (!"authorization_code".equals(grant_type)) {
            return error(HttpStatus.BAD_REQUEST, "unsupported_grant_type", "Only authorization_code is supported.");
        }

        AuthTransaction txn = store.transactionsByCode.get(code);
        if (txn == null || txn.used) {
            return error(HttpStatus.BAD_REQUEST, "invalid_request", "Authorization code is invalid or already used.");
        }
        if (Instant.now().isAfter(txn.expiresAt)) {
            return error(HttpStatus.BAD_REQUEST, "invalid_transaction", "Authorization session expired, restart login.");
        }
        if (!txn.clientId.equals(client_id) || !txn.redirectUri.equals(redirect_uri)) {
            return error(HttpStatus.BAD_REQUEST, "invalid_request",
                    "client_id or redirect_uri does not match the original authorization request.");
        }

        OidcClient client = store.clients.get(client_id);
        if (client == null) {
            return error(HttpStatus.BAD_REQUEST, "invalid_client", "Unknown client_id.");
        }

        if (txn.codeChallenge != null) {
            if (code_verifier == null) {
                return error(HttpStatus.BAD_REQUEST, "invalid_request", "code_verifier is required for this transaction.");
            }
            if (!codeChallengeS256(code_verifier).equals(txn.codeChallenge)) {
                return error(HttpStatus.BAD_REQUEST, "invalid_grant", "code_verifier does not match the original code_challenge.");
            }
        }

        if (client.publicJwk == null) {
            return error(HttpStatus.BAD_REQUEST, "invalid_client",
                    "No public JWK registered for this client - cannot verify client_assertion.");
        }
        if (client_assertion == null) {
            return error(HttpStatus.BAD_REQUEST, "invalid_request", "client_assertion is required.");
        }
        try {
            JWTClaimsSet assertionClaims = jwtService.verifyClientAssertion(client_assertion, client.publicJwk);
            if (!client_id.equals(assertionClaims.getIssuer()) || !client_id.equals(assertionClaims.getSubject())) {
                return error(HttpStatus.BAD_REQUEST, "invalid_assertion", "client_assertion iss/sub must equal client_id.");
            }
            boolean audienceOk = assertionClaims.getAudience() != null
                    && assertionClaims.getAudience().contains(TOKEN_ENDPOINT);
            if (!audienceOk) {
                return error(HttpStatus.BAD_REQUEST, "invalid_assertion",
                        "client_assertion aud must exactly equal the token endpoint: " + TOKEN_ENDPOINT);
            }
        } catch (Exception e) {
            return error(HttpStatus.BAD_REQUEST, "invalid_assertion", "client_assertion verification failed: " + e.getMessage());
        }

        txn.used = true;
        String psut = psutService.generate(txn.individualId, client.relyingPartyId);
        txn.partnerSpecificUserToken = psut;

        Instant now = Instant.now();
        String accessToken = jwtService.sign(new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(psut)
                .audience(client_id)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(ACCESS_TOKEN_TTL_SECONDS)))
                .jwtID(UUID.randomUUID().toString())
                .build());

        JWTClaimsSet.Builder idTokenBuilder = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(psut)
                .audience(client_id)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(ID_TOKEN_TTL_SECONDS)));
        if (txn.nonce != null) {
            idTokenBuilder.claim("nonce", txn.nonce);
        }
        String idToken = jwtService.sign(idTokenBuilder.build());

        store.transactionsByAccessToken.put(accessToken, txn);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("access_token", accessToken);
        response.put("id_token", idToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", ACCESS_TOKEN_TTL_SECONDS);
        return ResponseEntity.ok(response);
    }

    private String codeChallengeS256(String verifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String error, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("error_description", description);
        return ResponseEntity.status(status).body(body);
    }
}
