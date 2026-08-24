package et.fayda.mock.esignet.controller;

import et.fayda.mock.esignet.service.SigningKeyProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DiscoveryController {

    // Keep in sync with server.port in application.yml and the ISSUER constants
    // in TokenController / UserInfoController.
    private static final String BASE_URL = "http://localhost:7088";

    private final SigningKeyProvider keyProvider;

    public DiscoveryController(SigningKeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    @GetMapping("/.well-known/openid-configuration")
    public Map<String, Object> discovery() {
        return Map.ofEntries(
                Map.entry("issuer", BASE_URL),
                Map.entry("authorization_endpoint", BASE_URL + "/authorize"),
                Map.entry("token_endpoint", BASE_URL + "/oauth/v2/token"),
                Map.entry("userinfo_endpoint", BASE_URL + "/oidc/userinfo"),
                Map.entry("jwks_uri", BASE_URL + "/jwks.json"),
                Map.entry("response_types_supported", List.of("code")),
                Map.entry("grant_types_supported", List.of("authorization_code")),
                Map.entry("subject_types_supported", List.of("pairwise")),
                Map.entry("id_token_signing_alg_values_supported", List.of("RS256")),
                Map.entry("token_endpoint_auth_methods_supported", List.of("private_key_jwt")),
                Map.entry("token_endpoint_auth_signing_alg_values_supported", List.of("RS256")),
                Map.entry("claims_supported", List.of("name", "address", "gender", "birthdate",
                        "picture", "email", "phone_number", "individual_id",
                        "phone_number_verified", "registration_type", "updated_at")),
                Map.entry("claims_locales_supported", List.of("en", "am")),
                Map.entry("scopes_supported", List.of("openid", "profile", "email")),
                Map.entry("code_challenge_methods_supported", List.of("S256"))
        );
    }

    @GetMapping("/jwks.json")
    public Map<String, Object> jwks() {
        return Map.of("keys", List.of(keyProvider.getPublicJwk().toJSONObject()));
    }
}
