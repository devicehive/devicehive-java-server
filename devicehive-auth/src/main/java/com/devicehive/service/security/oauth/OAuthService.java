package com.devicehive.service.security.oauth;

/*
 * #%L
 * DeviceHive Java Server Common business logic
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.service.BaseNetworkService;
import com.devicehive.service.BaseUserService;
import com.devicehive.service.security.jwt.JwtTokenService;
import com.devicehive.vo.JwtTokenVO;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.UserVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

@Service
public class OAuthService {
    private static final Logger logger = LoggerFactory.getLogger(OAuthService.class);

    private final BaseUserService userService;
    private final JwtTokenService jwtTokenService;
    private final BaseNetworkService networkService;

    @Value("${google.clientId}")
    private String googleClientId;

    @Value("${github.clientId}")
    private String githubClientId;

    @Value("${github.clientSecret}")
    private String githubClientSecret;

    @Value("${github.login.url}")
    private String githubLoginUrl;

    @Value("${github.emails.url}")
    private String githubEmailsUrl;

    @Autowired
    public OAuthService(BaseUserService userService, JwtTokenService jwtTokenService, BaseNetworkService networkService) {
        this.userService = userService;
        this.jwtTokenService = jwtTokenService;
        this.networkService = networkService;
    }

    public JwtTokenVO authenticateGoogle(String accessToken) throws GeneralSecurityException, IOException {
        logger.debug("Performing google auth");
        final HttpTransport httpTransport = new NetHttpTransport();
        final JsonFactory jsonFactory = new JacksonFactory();

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(httpTransport, jsonFactory)
                .setAudience(Collections.singletonList(googleClientId))
                .setIssuer("accounts.google.com")
                .build();

        GoogleIdToken idToken = verifier.verify(accessToken);

        if (idToken == null) {
            throw new GeneralSecurityException("Google id token is null");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        Optional<UserVO> userOpt = userService.findByLogin(email);

        if (userOpt.isPresent()) {
            return jwtTokenService.createJwtToken(userOpt.get());
        } else {
            UserVO newUser = createNewOauthUser(email);
            return jwtTokenService.createJwtToken(newUser);
        }
    }

    public JwtTokenVO authenticateFacebook(String accessToken) throws GeneralSecurityException {
        logger.debug("Performing facebook auth");

        Facebook facebook = new FacebookTemplate(accessToken);
        String [] fields = { "id", "email"};
        org.springframework.social.facebook.api.User userProfile = facebook.fetchObject("me",
                org.springframework.social.facebook.api.User.class, fields);
        String email = userProfile.getEmail();
        Optional<UserVO> userOpt = userService.findByLogin(email);

        if (userOpt.isPresent()) {
            return jwtTokenService.createJwtToken(userOpt.get());
        } else {
            UserVO newUser = createNewOauthUser(email);
            return jwtTokenService.createJwtToken(newUser);
        }
    }

    public JwtTokenVO authenticateGithub(String code) throws GeneralSecurityException, IOException {
        logger.debug("Performing github auth");

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        Map<String, String> map = new HashMap<>();
        map.put("Accept", "application/json");
        headers.setAll(map);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", githubClientId);
        params.add("client_secret", githubClientSecret);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        final ResponseEntity<?> response = new RestTemplate().postForEntity(githubLoginUrl, request, HashMap.class);
        final String responseString = (String) ((HashMap) ((ResponseEntity) response).getBody()).get("access_token");

        final GitHubClient client = new GitHubClient();
        client.setOAuth2Token(responseString);

        final org.eclipse.egit.github.core.service.UserService githubService = new org.eclipse.egit.github.core.service.UserService(client);

        String email = githubService.getUser().getEmail();
        if (email == null) {

            email = getHiddenEmailFromGithub(responseString);

            if (email == null || email.equals("")) {
                email = githubService.getUser().getLogin();
            }
        }

        Optional<UserVO> userOpt = userService.findByLogin(email);

        if (userOpt.isPresent()) {
            return jwtTokenService.createJwtToken(userOpt.get());
        } else {
            UserVO newUser = createNewOauthUser(email);
            return jwtTokenService.createJwtToken(newUser);
        }
    }

    private String getHiddenEmailFromGithub(String token) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("User-Agent", "DeviceHive API");

        HttpEntity entity = new HttpEntity<>("parameters", httpHeaders);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<JsonNode> returnedInfo = restTemplate.exchange(String.format(githubEmailsUrl, token), HttpMethod.GET, entity, JsonNode.class);

        JsonNode node = returnedInfo.getBody();

        String username = "";
        for (JsonNode elementNode : node) {
            if (!elementNode.get("primary").booleanValue()) {
                continue;
            }
            username = elementNode.get("email").asText();
        }

        return username;
    }

    private UserVO createNewOauthUser(String email) {
        UserVO newUser = new UserVO();
        newUser.setLogin(email);
        newUser.setRole(UserRole.CLIENT);
        newUser.setStatus(UserStatus.ACTIVE);
        newUser.setLoginAttempts(0);
        newUser = userService.createUser(newUser, UUID.randomUUID().toString());

        networkService.createOrUpdateNetworkByUser(newUser);

        return newUser;
    }
}
