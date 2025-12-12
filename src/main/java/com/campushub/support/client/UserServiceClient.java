package com.campushub.support.client;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class UserServiceClient {

    private final WebClient webClient;

    public UserServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("lb://campushub-user-service").build();
    }

    public List<UserDto> getUsersByDepartment(String department, String token) {
        return webClient.get()
                .uri("/api/users/department/{department}", department)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<UserDto>>() {})
                .block();
    }

    public UserDto getUserById(Long userId, String token) {
        return webClient.get()
                .uri("/api/users/{userId}", userId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(UserDto.class)
                .block();
    }
}
