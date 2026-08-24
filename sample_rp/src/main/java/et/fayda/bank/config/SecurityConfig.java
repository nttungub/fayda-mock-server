package et.fayda.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient;
    private final OAuth2UserService<OidcUserRequest, OidcUser> faydaUserService;

    public SecurityConfig(
            OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> accessTokenResponseClient,
            OAuth2UserService<OidcUserRequest, OidcUser> faydaUserService) {
        this.accessTokenResponseClient = accessTokenResponseClient;
        this.faydaUserService = faydaUserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/css/**", "/images/**", "/coming-soon",
                                "/unverified-dashboard").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .loginPage("/")
                        .defaultSuccessUrl("/dashboard", true)
                        .redirectionEndpoint(redir -> redir
                                .baseUri("/callback")
                        )
                        .tokenEndpoint(token -> token
                                .accessTokenResponseClient(accessTokenResponseClient)
                        )
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(faydaUserService)
                        )
                        .failureHandler((request, response, exception) -> {
                            System.out.println("========== OAUTH LOGIN FAILED ==========");
                            System.out.println("Message: " + exception.getMessage());
                            if (exception instanceof OAuth2AuthenticationException oae) {
                                System.out.println("Error code:  " + oae.getError().getErrorCode());
                                System.out.println("Description: " + oae.getError().getDescription());
                            }
                            System.out.println("========================================");
                            response.sendRedirect("/?error");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                );
        return http.build();
    }
}