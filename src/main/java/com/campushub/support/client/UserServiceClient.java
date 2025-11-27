package com.campushub.support.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class UserServiceClient {

    private final WebClient webClient;
    private final String userServiceBaseUrl;

    public UserServiceClient(WebClient.Builder webClientBuilder,
                             @Value("${application.userservice.url:http://campushub-user-service:8081}") String userServiceBaseUrl) {
        this.userServiceBaseUrl = userServiceBaseUrl;
        this.webClient = webClientBuilder.baseUrl(userServiceBaseUrl).build();
    }

    public boolean isUserValid(Long userId) {
        // This is a basic check. In a real application, you might check if the user exists
        // and if they have the expected role (e.g., TEACHER)
        try {
            // TODO: Pass JWT token from current request for inter-service communication securely
            // For now, this endpoint in user-service is authenticated, so it would fail without a token.
            // We need a system-to-system token or a token propagation mechanism.
            // For now, let's assume this endpoint is publicly accessible or called with a system token.
            return webClient.get()
                    .uri("/api/users/{id}", userId)
                    .retrieve()
                    .toBodilessEntity()
                    .map(response -> response.getStatusCode().is2xxSuccessful())
                    .onErrorResume(e -> {
                        // Log error, e.g., user not found, communication issue
                        return Mono.just(false);
                    })
                    .block(); 
        } catch (Exception e) {
            // Log the exception
            return false;
        }
    }

    public Long getUserIdByUsername(String username) {
        try {
            // TODO: Pass JWT token from current request for inter-service communication securely
            // For now, this endpoint in user-service is authenticated.
            // A dedicated internal endpoint or system token is needed.
            // Assuming for now, /api/users endpoint would allow access to get user details by admin.
            UserResponseDto userDto = webClient.get()
                    .uri("/api/users?username={username}", username) // Assuming user-service has an endpoint to find by username
                    // This endpoint needs to be secured, e.g. for ADMIN or internal access
                    // .header("Authorization", "Bearer " + getSystemOrUserToken())
                    .retrieve()
                    .bodyToMono(UserResponseDto.class) // Expecting a single user or list
                    .block();
            return userDto != null ? userDto.getId() : null;
        } catch (Exception e) {
            // Log the exception
            return null;
        }
    }

    // Nested DTO for user-service response
    public static class UserResponseDto {
        private Long id;
        private String username;
        private String fullName;

        public UserResponseDto() {
        }

        public UserResponseDto(Long id, String username, String fullName) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
        // ... other fields if needed
    }
}
