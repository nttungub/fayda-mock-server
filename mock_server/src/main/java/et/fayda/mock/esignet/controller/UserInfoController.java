package et.fayda.mock.esignet.controller;

import com.nimbusds.jwt.JWTClaimsSet;
import et.fayda.mock.esignet.model.AuthTransaction;
import et.fayda.mock.esignet.model.MockIdentity;
import et.fayda.mock.esignet.service.ClaimsService;
import et.fayda.mock.esignet.service.JwtService;
import et.fayda.mock.esignet.store.DataStore;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class UserInfoController {

    // Keep in sync with server.port in application.yml and BASE_URL in DiscoveryController.
    private static final String ISSUER = "http://localhost:7088";

    private final DataStore store;
    private final JwtService jwtService;
    private final ClaimsService claimsService;

    public UserInfoController(DataStore store, JwtService jwtService, ClaimsService claimsService) {
        this.store = store;
        this.jwtService = jwtService;
        this.claimsService = claimsService;
    }

    @GetMapping(value = "/oidc/userinfo", produces = "application/jwt")
    public ResponseEntity<String> userinfo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"error\":\"invalid_token\"}");
        }
        String accessToken = authorization.substring("Bearer ".length());
        AuthTransaction txn = store.transactionsByAccessToken.get(accessToken);
        if (txn == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"error\":\"invalid_token\",\"error_description\":\"Unknown or expired access token\"}");
        }

        MockIdentity identity = store.identities.get(txn.individualId);
        if (identity == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\":\"identity_not_found\"}");
        }

        List<String> requestedClaims = resolveRequestedClaims(txn);
        List<String> locales = resolveLocales(txn.claimsLocales);
        Map<String, Object> claims = claimsService.buildClaims(identity, requestedClaims, locales);

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .issuer(ISSUER)
                .subject(txn.partnerSpecificUserToken)
                .audience(txn.clientId);
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            builder.claim(entry.getKey(), entry.getValue());
        }

        String signedJwt = jwtService.sign(builder.build());
        return ResponseEntity.ok().contentType(MediaType.valueOf("application/jwt")).body(signedJwt);
    }

    private List<String> resolveRequestedClaims(AuthTransaction txn) {
        Set<String> claims = new LinkedHashSet<>();
        Object userinfoClaimsObj = txn.claimsRequest == null ? null : txn.claimsRequest.get("userinfo");
        if (userinfoClaimsObj instanceof Map<?, ?> userinfoClaims) {
            for (Object key : userinfoClaims.keySet()) {
                claims.add(String.valueOf(key));
            }
        } else {
            String scope = txn.scope == null ? "" : txn.scope;
            if (scope.contains("profile")) {
                claims.addAll(List.of("name", "gender", "birthdate", "picture", "address", "individual_id"));
            }
            if (scope.contains("email")) {
                claims.add("email");
            }
            if (scope.contains("phone") || scope.contains("resident-service")) {
                claims.add("phone_number");
            }
        }
        return new ArrayList<>(claims);
    }

    private List<String> resolveLocales(String claimsLocales) {
        if (claimsLocales == null || claimsLocales.isBlank()) {
            return List.of("en");
        }
        return Arrays.asList(claimsLocales.trim().split("\\s+"));
    }
}
