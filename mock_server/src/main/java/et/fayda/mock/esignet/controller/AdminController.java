package et.fayda.mock.esignet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import et.fayda.mock.esignet.model.MockIdentity;
import et.fayda.mock.esignet.model.OidcClient;
import et.fayda.mock.esignet.store.DataStore;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Setup endpoints - not part of the OIDC spec. Use these to register your app
 * and create fake citizens before exercising the real /authorize -> /oauth/v2/token
 * -> /oidc/userinfo flow.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final DataStore store;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdminController(DataStore store) {
        this.store = store;
    }

    /**
     * Register a relying party client.
     * Body example:
     * {
     *   "clientId": "my-test-app",
     *   "relyingPartyId": "my-test-app",
     *   "redirectUris": ["http://localhost:3000/callback"],
     *   "publicJwk": { "kty": "RSA", "n": "...", "e": "AQAB", ... }
     * }
     */
    @PostMapping("/clients")
    public Map<String, Object> registerClient(@RequestBody Map<String, Object> body) {
        String clientId = (String) body.getOrDefault("clientId", UUID.randomUUID().toString());
        String relyingPartyId = (String) body.getOrDefault("relyingPartyId", clientId);
        OidcClient client = new OidcClient(clientId, relyingPartyId);

        Object redirectUris = body.get("redirectUris");
        if (redirectUris instanceof Iterable<?> uris) {
            for (Object uri : uris) {
                client.redirectUris.add(String.valueOf(uri));
            }
        }

        Object publicJwk = body.get("publicJwk");
        if (publicJwk != null) {
            client.publicJwk = toJson(publicJwk);
        }

        store.clients.put(clientId, client);
        store.persist();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("clientId", clientId);
        response.put("relyingPartyId", relyingPartyId);
        response.put("redirectUris", client.redirectUris);
        response.put("message", "Client registered. Use this clientId when building your /authorize URL.");
        return response;
    }

    @GetMapping("/clients")
    public Map<String, OidcClient> listClients() {
        return store.clients;
    }

    /**
     * Create a fake citizen identity for testing.
     * Body example:
     * {
     *   "individualId": "1234567890123456",
     *   "pin": "1111",
     *   "name": {"en": "Test User", "am": "..."},
     *   "email": "test@example.com",
     *   "phoneNumber": "+251900000000",
     *   "gender": "Male",
     *   "birthdate": "2000-01-01",
     *   "picture": "https://placehold.co/150",
     *   "region": {"en": "Addis Ababa"},
     *   "zone": {"en": "Bole"},
     *   "woreda": {"en": "Woreda 01"},
     *   "kebele": {"en": "Kebele 01"}
     * }
     * Any field can be omitted; individualId and pin get generated defaults if missing.
     */
    @PostMapping("/identities")
    public Map<String, Object> createIdentity(@RequestBody Map<String, Object> body) {
        String individualId = (String) body.getOrDefault("individualId", randomIndividualId());
        String pin = (String) body.getOrDefault("pin", "1234");
        MockIdentity identity = new MockIdentity(individualId, pin);

        putLocaleMap(identity.name, body.get("name"));
        identity.email = (String) body.get("email");
        identity.phoneNumber = (String) body.get("phoneNumber");
        identity.gender = (String) body.get("gender");
        identity.birthdate = (String) body.get("birthdate");
        identity.picture = (String) body.get("picture");
        putLocaleMap(identity.region, body.get("region"));
        putLocaleMap(identity.zone, body.get("zone"));
        putLocaleMap(identity.woreda, body.get("woreda"));
        putLocaleMap(identity.kebele, body.get("kebele"));

        store.identities.put(individualId, identity);
        store.persist();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("individualId", individualId);
        response.put("pin", pin);
        response.put("message", "Identity created. Use these as the login credentials at /authorize.");
        return response;
    }

    @GetMapping("/identities")
    public Map<String, MockIdentity> listIdentities() {
        return store.identities;
    }

    private void putLocaleMap(Map<String, String> target, Object source) {
        if (source instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                target.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
        }
    }

    private String randomIndividualId() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid publicJwk - must be a valid JWK JSON object", e);
        }
    }
}
