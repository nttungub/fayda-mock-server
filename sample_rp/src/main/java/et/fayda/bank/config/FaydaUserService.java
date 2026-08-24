package et.fayda.bank.config;

import com.nimbusds.jwt.SignedJWT;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Component
public class FaydaUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        String userInfoUri = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUri();
        String accessToken = userRequest.getAccessToken().getTokenValue();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(userInfoUri))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String body = response.body().trim();

            SignedJWT signedJWT = SignedJWT.parse(body);
            Map<String, Object> claims = signedJWT.getJWTClaimsSet().getClaims();

            OidcUserInfo userInfo = new OidcUserInfo(claims);

            return new DefaultOidcUser(
                    null,
                    userRequest.getIdToken(),
                    userInfo,
                    "sub"
            );

        } catch (OAuth2AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new OAuth2AuthenticationException(
                    "Failed to load Fayda userinfo: " + e.getMessage());
        }
    }
}