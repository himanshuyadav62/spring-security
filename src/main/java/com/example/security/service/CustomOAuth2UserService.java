package com.example.security.service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.security.entity.CustomOAuth2User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final String GITHUB_EMAILS_API_URL = "https://api.github.com/user/emails";

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        System.out.println("Oauth2User " + oAuth2User);
        System.out.println("Token Type: " + userRequest.getAccessToken().getTokenType().getValue());
        System.out.println("Access Token: " + userRequest.getAccessToken().getTokenValue());
        

        // Fetch user's email from OAuth2 provider
        String email = extractEmailFromOAuth2User(oAuth2User, userRequest);

        // Fetch roles from the database based on the email
        Set<String> roles = fetchRolesFromDb(email);


        // Set user authentication in SecurityContextHolder
        setAuthentication(email, roles);

        return new CustomOAuth2User(oAuth2User,email, roles);
    }

    private String extractEmailFromOAuth2User(OAuth2User oAuth2User, OAuth2UserRequest userRequest) {
        String email = null;
        if (userRequest.getClientRegistration().getRegistrationId().equals("google")) {
            // Extract email from Google OAuth2 user attributes
            email = (String) oAuth2User.getAttributes().get("email");
        } else if (userRequest.getClientRegistration().getRegistrationId().equals("github")) {
            // Fetch email from GitHub API
            email = fetchGitHubEmail(userRequest);
        }
        return email;
    }

    private String fetchGitHubEmail(OAuth2UserRequest userRequest) {
        RestTemplate restTemplate = new RestTemplate();

        // Prepare the HTTP headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + userRequest.getAccessToken().getTokenValue());
        headers.set("Accept", "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            GITHUB_EMAILS_API_URL,
            HttpMethod.GET,
            entity,
            String.class
        );

        
        if (response.getStatusCode() == HttpStatus.OK) {
            String jsonResponse = response.getBody();
            return extractPrimaryEmailFromResponse(jsonResponse);
        }
        return null; // If no email is found, return null or handle appropriately
    }
    private String extractPrimaryEmailFromResponse(String jsonResponse) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode emailsNode = objectMapper.readTree(jsonResponse);
            for (JsonNode emailNode : emailsNode) {
                if (emailNode.get("primary").asBoolean()) {
                    return emailNode.get("email").asText();
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); 
        }

        return null;
    }


    private Set<String> fetchRolesFromDb(String email) {
        // Replace with actual logic to fetch roles from your database
        return Set.of("ROLE_USER");  // For example purposes, returning a single role
    }

    private void setAuthentication(String email, Set<String> roles) {
        // Create authorities based on roles
        Set<SimpleGrantedAuthority> authorities = roles.stream()
            .map(SimpleGrantedAuthority::new)
            .collect(Collectors.toSet());

        // Create an authenticated user
        User principal = new User(email, "", authorities);

        // Create an Authentication token
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
        // Set authentication in the SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
