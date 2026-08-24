package et.fayda.mock.esignet.model;

import java.time.Instant;
import java.util.Map;

/**
 * Tracks one authorization attempt from the initial /authorize request through
 * the mock login form, the authorization code, and finally the issued access token.
 */
public class AuthTransaction {

    public String transactionId;
    public String clientId;
    public String redirectUri;
    public String scope;
    public String state;
    public String nonce;
    public String codeChallenge;
    public String codeChallengeMethod;
    public String claimsLocales;
    public Map<String, Object> claimsRequest;

    public String code;
    public String individualId;
    public String partnerSpecificUserToken;
    public boolean used = false;
    public Instant expiresAt;
}
