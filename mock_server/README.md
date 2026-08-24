# Fayda Mock eSignet

A local Spring Boot server that simulates the VeriFayda 2.0 / eSignet OIDC flow
(authorize -> token -> userinfo) for **integration testing only**.

**Not affiliated with Ethiopia's National ID Program, MOSIP, or Fayda.** All identity
data is fictional test data. Nothing here talks to any real Fayda service - it's a
standalone stand-in you run entirely on your own machine.

Based on details reverse-engineered from public MOSIP eSignet source and several
Fayda relying-party reference apps: pairwise `sub` (PSUT) derivation, PKCE, JWT
client-assertion auth, multi-locale claim suffixing (`name#en` / `name#am`), and the
Ethiopian-specific `address` shape (region/zone/woreda/kebele).

## What it implements

| Endpoint | Purpose |
|---|---|
| `GET /.well-known/openid-configuration` | OIDC discovery document |
| `GET /jwks.json` | This server's public signing key |
| `GET /authorize` | Authorization endpoint - shows a mock login form |
| `POST /authorize/login` | Completes login, redirects back with `?code=...` |
| `POST /oauth/v2/token` | Token exchange - validates PKCE + `client_assertion`, returns `access_token` + `id_token` |
| `GET /oidc/userinfo` | Returns claims as a signed JWT |
| `POST /admin/clients` | Register a relying party (your app) |
| `GET /admin/clients` | List registered clients |
| `POST /admin/identities` | Create a fake citizen for testing |
| `GET /admin/identities` | List identities (two are seeded automatically) |

Two demo identities are seeded on startup - check `GET /admin/identities` for their
`individualId` and `pin`.

## Web console

Once the server is running, open `http://localhost:7088/` for a browser UI over
the endpoints above: register clients (with an in-browser RS256 keypair generator,
no `npm install jose` needed), create identities, and run the whole
authorize -> token -> userinfo flow end to end without `curl`.

## Prerequisites

- Java 17+
- Maven (or your IDE's bundled Maven)
- Optional, for generating a client keypair: Node.js + `npm install jose`

## 1. Run the server

```bash
mvn spring-boot:run
```

Runs on `http://localhost:7088`. If you change `server.port` in `application.yml`,
also update the `ISSUER`/`BASE_URL` constants hardcoded in `DiscoveryController`,
`TokenController`, and `UserInfoController` to match.

## 2. Generate a client keypair

```bash
npm install jose
node generate-client-keys.mjs
```

This prints a `PUBLIC_JWK` (for step 3) and a `PRIVATE_KEY_BASE64` (for your own
relying party app to sign `client_assertion` JWTs with, exactly like `PRIVATE_KEY_BASE64`
in the real Fayda reference apps).

## 3. Register your relying party

```bash
curl -X POST http://localhost:7088/admin/clients \
  -H "Content-Type: application/json" \
  -d '{
    "clientId": "my-test-app",
    "relyingPartyId": "my-test-app",
    "redirectUris": ["http://localhost:3000/callback"],
    "publicJwk": { "kty": "RSA", "n": "...", "e": "AQAB", "kid": "mock-client-key-1", "use": "sig", "alg": "RS256" }
  }'
```

Paste in the full `PUBLIC_JWK` JSON printed in step 2.

## 4. Check available mock identities

```bash
curl http://localhost:7088/admin/identities
```

Note an `individualId` and its `pin`, or create your own:

```bash
curl -X POST http://localhost:7088/admin/identities \
  -H "Content-Type: application/json" \
  -d '{
    "individualId": "1111222233334444",
    "pin": "0000",
    "name": {"en": "Test User"},
    "email": "test@example.com",
    "phoneNumber": "+251900000000",
    "gender": "Male",
    "birthdate": "2000-01-01"
  }'
```

## 5. Build the authorize URL and log in

Open in a browser (PKCE shown here with a fixed example verifier - generate your own
per attempt in real code):

```
http://localhost:7088/authorize?response_type=code&client_id=my-test-app&redirect_uri=http://localhost:3000/callback&scope=openid%20profile%20email&state=xyz&nonce=abc&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&code_challenge_method=S256&claims_locales=en%20am
```

Pick the mock identity, enter its PIN, submit. You'll land on
`http://localhost:3000/callback?code=...&state=xyz` (that URL doesn't need to actually
exist yet - just copy the `code` from the address bar).

## 6. Exchange the code for tokens

Your relying party backend signs a `client_assertion` JWT (`iss`/`sub` = client_id,
`aud` = `http://localhost:7088/oauth/v2/token`, short expiry) using the private key
from step 2 - see `references/nodejs.md`-style code in the Fayda eSignet skill for a
working example - then:

```bash
curl -X POST http://localhost:7088/oauth/v2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "code=<code from step 5>" \
  -d "redirect_uri=http://localhost:3000/callback" \
  -d "client_id=my-test-app" \
  -d "client_assertion=<your signed JWT>" \
  -d "client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer" \
  -d "code_verifier=<the code_verifier matching your code_challenge>"
```

Returns `access_token`, `id_token`, `token_type`, `expires_in`.

## 7. Fetch userinfo

```bash
curl http://localhost:7088/oidc/userinfo -H "Authorization: Bearer <access_token>"
```

Returns a signed JWT - decode the payload (e.g. `jwt.io`, or split on `.` and
base64url-decode the middle segment) to see the claims.

## Notes and limitations

- **Pairwise `sub`**: computed as `base64url(SHA3-256(individualId + relyingPartyId))`,
  matching the real MOSIP mock-identity-system algorithm - same citizen, different
  relying party, different `sub`.
- **Multi-locale claims**: request 2+ `claims_locales` (e.g. `en am`) and fields like
  `name` come back suffixed as `name#en` / `name#am` instead of a plain key.
- **No real OTP/biometrics** - login is just picking an identity + entering its PIN.
- **No consent screen, no rate limiting, no HTTPS, minimal input hardening** - this is
  a disposable local dev tool, not something to expose publicly or use with real data.
- **Clients and identities persist across restarts** - every `POST /admin/clients` or
  `POST /admin/identities` call is written to `data/mock-store.json` (path configurable
  via `mock.store.file`), and that file is reloaded on the next startup. The two demo
  identities are only seeded (and the file only created) the very first time the server
  runs, i.e. when no store file exists yet. Note that PINs are stored in plaintext in
  that file - fine for fake test data, not a security control.
- **In-flight transactions are still in-memory only** - an authorization code or access
  token issued before a restart won't be valid after one; only client/identity
  *registrations* survive.
- I wrote and reviewed this code carefully but could not run `mvn compile` in the
  environment I built it in - it doesn't have network access to Maven Central. Please
  run `mvn clean install` and let me know if anything needs fixing.
