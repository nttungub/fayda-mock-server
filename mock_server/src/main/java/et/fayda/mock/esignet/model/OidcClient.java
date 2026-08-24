package et.fayda.mock.esignet.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A relying party (your app) registered against this mock server.
 *
 * relyingPartyId is deliberately separate from clientId, mirroring MOSIP's real model
 * where one partner/organization (relyingPartyId) can register several OIDC clients
 * (e.g. a web app and a mobile app). The pairwise "sub" is derived from relyingPartyId,
 * so two clients that share the same relyingPartyId will see the same sub for a given
 * identity, while two different relyingPartyIds will see different subs.
 */
public class OidcClient {

    public final String clientId;
    public final String relyingPartyId;
    public final Set<String> redirectUris = new LinkedHashSet<>();

    /** JSON string of the client's RSA public JWK, used to verify client_assertion signatures. */
    public String publicJwk;

    @JsonCreator
    public OidcClient(@JsonProperty("clientId") String clientId,
                       @JsonProperty("relyingPartyId") String relyingPartyId) {
        this.clientId = clientId;
        this.relyingPartyId = relyingPartyId;
    }
}
