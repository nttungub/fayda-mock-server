package et.fayda.mock.esignet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import et.fayda.mock.esignet.model.AuthTransaction;
import et.fayda.mock.esignet.model.MockIdentity;
import et.fayda.mock.esignet.model.OidcClient;
import et.fayda.mock.esignet.store.DataStore;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

@RestController
public class AuthorizeController {

    private final DataStore store;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AuthorizeController(DataStore store) {
        this.store = store;
    }

    @GetMapping(value = "/authorize", produces = MediaType.TEXT_HTML_VALUE)
    public String authorize(@RequestParam String client_id,
                             @RequestParam String redirect_uri,
                             @RequestParam(required = false) String scope,
                             @RequestParam(required = false) String state,
                             @RequestParam(required = false) String nonce,
                             @RequestParam(required = false) String code_challenge,
                             @RequestParam(required = false) String code_challenge_method,
                             @RequestParam(required = false) String claims,
                             @RequestParam(required = false) String claims_locales,
                             @RequestParam(required = false) String acr_values) {

        OidcClient client = store.clients.get(client_id);
        if (client == null) {
            return errorHtml("unknown_client",
                    "No client is registered with client_id=" + escapeHtml(client_id)
                            + ". POST to /admin/clients first - see README.md.");
        }
        if (!client.redirectUris.isEmpty() && !client.redirectUris.contains(redirect_uri)) {
            return errorHtml("invalid_redirect_uri",
                    "redirect_uri does not match any URI registered for this client.");
        }

        AuthTransaction txn = new AuthTransaction();
        txn.transactionId = UUID.randomUUID().toString();
        txn.clientId = client_id;
        txn.redirectUri = redirect_uri;
        txn.scope = scope;
        txn.state = state;
        txn.nonce = nonce;
        txn.codeChallenge = code_challenge;
        txn.codeChallengeMethod = code_challenge_method;
        txn.claimsLocales = claims_locales;
        txn.expiresAt = Instant.now().plus(10, ChronoUnit.MINUTES);

        if (claims != null && !claims.isBlank()) {
            try {
                txn.claimsRequest = objectMapper.readValue(claims, Map.class);
            } catch (Exception e) {
                return errorHtml("invalid_claims", "Could not parse the 'claims' parameter as JSON.");
            }
        }

        store.transactionsById.put(txn.transactionId, txn);

        StringBuilder identityOptions = new StringBuilder();
        for (MockIdentity identity : store.identities.values()) {
            String display = identity.name.getOrDefault("en", identity.individualId);
            identityOptions.append("<option value=\"").append(escapeHtml(identity.individualId)).append("\">")
                    .append(escapeHtml(display)).append(" (").append(escapeHtml(identity.individualId))
                    .append(")</option>");
        }
        if (identityOptions.isEmpty()) {
            identityOptions.append("<option disabled>No identities yet - POST to /admin/identities first</option>");
        }

        return """
                <html>
                <head><title>Fayda Mock eSignet - Sign In</title>
                <style>
                  body { font-family: sans-serif; max-width: 480px; margin: 60px auto; color: #222; }
                  .card { border: 1px solid #ddd; border-radius: 8px; padding: 24px; }
                  h2 { margin-top: 0; }
                  label { display: block; margin-top: 12px; font-size: 14px; color: #333; }
                  select, input { width: 100%%; padding: 8px; margin-top: 4px; box-sizing: border-box; }
                  button { margin-top: 20px; width: 100%%; padding: 10px; background: #0b6b3a; color: #fff;
                           border: none; border-radius: 4px; cursor: pointer; font-size: 15px; }
                  .app-name { color: #0b6b3a; font-weight: 600; }
                  .hint { font-size: 12px; color: #777; margin-top: 4px; }
                </style>
                </head>
                <body>
                  <div class="card">
                    <h2>\uD83C\uDDEA\uD83C\uDDF9 Fayda Mock eSignet</h2>
                    <p><span class="app-name">%s</span> is requesting to verify your identity.</p>
                    <form method="post" action="/authorize/login">
                      <input type="hidden" name="transactionId" value="%s" />
                      <label>Mock identity</label>
                      <select name="individualId">%s</select>
                      <label>PIN</label>
                      <input type="password" name="pin" placeholder="PIN for the selected identity" />
                      <div class="hint">Check GET /admin/identities for each identity's PIN.</div>
                      <button type="submit">Authenticate</button>
                    </form>
                  </div>
                </body>
                </html>
                """.formatted(escapeHtml(client_id), txn.transactionId, identityOptions.toString());
    }

    @PostMapping("/authorize/login")
    public ResponseEntity<?> login(@RequestParam String transactionId,
                                    @RequestParam String individualId,
                                    @RequestParam String pin) {
        AuthTransaction txn = store.transactionsById.get(transactionId);
        if (txn == null || Instant.now().isAfter(txn.expiresAt)) {
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_HTML)
                    .body(errorHtml("invalid_transaction", "Session expired or unknown. Restart the login flow."));
        }

        MockIdentity identity = store.identities.get(individualId);
        if (identity == null || !identity.pin.equals(pin)) {
            return redirectWithError(txn.redirectUri, txn.state, "access_denied", "Invalid identity or PIN.");
        }

        txn.individualId = individualId;
        txn.code = UUID.randomUUID().toString();
        store.transactionsByCode.put(txn.code, txn);

        String redirect = buildRedirect(txn.redirectUri, "code", txn.code, txn.state);
        return ResponseEntity.status(302).location(URI.create(redirect)).build();
    }

    private ResponseEntity<Void> redirectWithError(String redirectUri, String state, String error, String description) {
        StringBuilder sb = new StringBuilder(redirectUri);
        sb.append(redirectUri.contains("?") ? "&" : "?");
        sb.append("error=").append(URLEncoder.encode(error, StandardCharsets.UTF_8));
        sb.append("&error_description=").append(URLEncoder.encode(description, StandardCharsets.UTF_8));
        if (state != null) {
            sb.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        }
        return ResponseEntity.status(302).location(URI.create(sb.toString())).build();
    }

    private String buildRedirect(String redirectUri, String paramName, String paramValue, String state) {
        StringBuilder sb = new StringBuilder(redirectUri);
        sb.append(redirectUri.contains("?") ? "&" : "?");
        sb.append(paramName).append("=").append(URLEncoder.encode(paramValue, StandardCharsets.UTF_8));
        if (state != null) {
            sb.append("&state=").append(URLEncoder.encode(state, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private String errorHtml(String error, String description) {
        return "<html><body><h3>" + escapeHtml(error) + "</h3><p>" + escapeHtml(description) + "</p></body></html>";
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
