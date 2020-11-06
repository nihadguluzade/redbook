package io.github.nihadguluzade.redbook.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.nihadguluzade.redbook.security.AccessTokenProvider;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AccessTokenResponseClient implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private RestOperations restOperations;

    public AccessTokenResponseClient(RestOperations restOperations) {
        this.restOperations = restOperations;
    }

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest oAuth2AuthorizationCodeGrantRequest) throws OAuth2AuthenticationException {
        ClientRegistration clientRegistration = oAuth2AuthorizationCodeGrantRequest.getClientRegistration();
        HttpHeaders headers = new HttpHeaders();

        headers.setBasicAuth(clientRegistration.getClientId(), clientRegistration.getClientSecret());
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(HttpHeaders.USER_AGENT, OAuth2Configuration.getUserAgent());

        String tokenUri = clientRegistration.getProviderDetails().getTokenUri();
        String grantType = "authorization_code";
        String code = oAuth2AuthorizationCodeGrantRequest.getAuthorizationExchange().getAuthorizationResponse().getCode();
        String redirectUri = "http://localhost:8080/login/oauth2/code/reddit";

        ResponseEntity<AccessResponse> response = restOperations.exchange(tokenUri, HttpMethod.POST, new HttpEntity<>("grant_type=" + grantType + "&code=" + code + "&redirect_uri=" + redirectUri, headers), AccessResponse.class);

        AccessResponse accessResponse = response.getBody();

        Set<String> scopes = accessResponse.getScopes().isEmpty() ?
                oAuth2AuthorizationCodeGrantRequest.getAuthorizationExchange().getAuthorizationRequest().getScopes() : accessResponse.getScopes();

        AccessTokenProvider.setOauth2Token(accessResponse.getAccessToken());

        return OAuth2AccessTokenResponse.withToken(accessResponse.getAccessToken())
                .tokenType(accessResponse.getTokenType())
                .expiresIn(accessResponse.getExpiresIn())
                .scopes(scopes)
                .build();

    }

    static class AccessResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("token_type")
        private String tokenType;

        @JsonProperty("expires_in")
        private int expiresIn;

        @JsonProperty("refresh_token")
        private String refreshToken;

        private String scope;

        public AccessResponse() {}

        public AccessResponse(String accessToken, String tokenType, int expiresIn, String refreshToken, String scope) {
            this.accessToken = accessToken;
            this.tokenType = tokenType;
            this.expiresIn = expiresIn;
            this.refreshToken = refreshToken;
            this.scope = scope;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public OAuth2AccessToken.TokenType getTokenType() {
            return OAuth2AccessToken.TokenType.BEARER.getValue().equalsIgnoreCase(tokenType) ? OAuth2AccessToken.TokenType.BEARER : null;
        }

        public int getExpiresIn() {
            return expiresIn;
        }

        public Set<String> getScopes() {
            return StringUtils.isEmpty(scope) ? Collections.emptySet() : Stream.of(scope.split("\\s+")).collect(Collectors.toSet());
        }
    }

}
