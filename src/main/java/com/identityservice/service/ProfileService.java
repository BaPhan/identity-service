package com.identityservice.service;

import com.identityservice.Utils.Scope;
import com.identityservice.dto.identity.Credential;
import com.identityservice.dto.identity.TokenExchangeParam;
import com.identityservice.dto.identity.UserCreationParam;
import com.identityservice.dto.request.LoginRequest;
import com.identityservice.dto.request.RegistrationRequest;
import com.identityservice.dto.response.LoginResponse;
import com.identityservice.dto.response.ProfileResponse;
import com.identityservice.exception.AppException;
import com.identityservice.exception.ErrorCode;
import com.identityservice.exception.ErrorNormalizer;
import com.identityservice.mapper.ProfileMapper;
import com.identityservice.repository.IdentityClient;
import com.identityservice.repository.ProfileRepository;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileService {
    ProfileRepository profileRepository;
    ProfileMapper profileMapper;
    IdentityClient identityClient;
    ErrorNormalizer errorNormalizer;

    @Value("${keycloak.client-id}")
    @NonFinal
    String clientId;
    @Value("${keycloak.client-secret}")
    @NonFinal
    String clientSecret;

    public ProfileResponse getMyProfile() {
        var userId = SecurityContextHolder.getContext().getAuthentication().getName();
        var profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        return profileMapper.toProfileResponse(profile);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<ProfileResponse> getAllProfiles() {
        var profiles = profileRepository.findAll();
        var userId = SecurityContextHolder.getContext().getAuthentication().getName();
        System.out.println(userId);
        return profiles.stream().map(profileMapper::toProfileResponse).toList();
    }

    public ProfileResponse register(RegistrationRequest request) {

        try {
            //create account in keycloak
            //exchange client token
            var token = identityClient.exchangeToken(TokenExchangeParam.builder()
                    .grant_type(Scope.CLIENT_CREDENTIALS)
                    .client_id(clientId)
                    .client_secret(clientSecret)
                    .scope(Scope.OPENID)
                    .build());
            log.info("token: {}", token);
            String toek = "Bearer " + token.getAccessToken();

            //create user with client token and given info
            var creationResponse = identityClient.createUser(toek, UserCreationParam.builder()
                    .username(request.getUsername())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .emailVerified(false)
                    .enabled(true)
                    .credentials(List.of(Credential.builder()
                            .type("password")
                            .value(request.getPassword())
                            .temporary(false)
                            .build()))
                    .build());

            //get user id of keycloak account
            String userId = extractUserId(creationResponse);
            log.info("Created user with id {}", userId);
            var profile = profileMapper.toProfile(request);
            profile.setUserId(userId);
            profile = profileRepository.save(profile);

            return profileMapper.toProfileResponse(profile);
        } catch (FeignException e) {
            throw errorNormalizer.handleKeyCloakException(e);
        }
    }

    public LoginResponse login(LoginRequest request) {
        try {
            var token = identityClient.exchangeToken(TokenExchangeParam.builder()
                    .grant_type(Scope.PASSWORD)
                    .client_id(clientId)
                    .client_secret(clientSecret)
                    .scope(Scope.OPENID)
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .build());
            log.info("token: {}", token);
            return LoginResponse.builder()
                    .accessToken(token.getAccessToken())
                    .refreshToken(token.getRefreshToken())
                    .expiresIn(token.getExpiresIn())
                    .refreshExpiresIn(token.getRefreshExpiresIn())
                    .build();
        } catch (FeignException e) {
            throw errorNormalizer.handleKeyCloakException(e);
        }
    }



    private String extractUserId(ResponseEntity<?> response) {
        String location = response.getHeaders().get("location").get(0).toString();
        String[] split = location.split("/");
        return split[split.length - 1];
    }
}
