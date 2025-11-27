package com.campushub.support.security;

import com.campushub.support.client.UserServiceClient;
import com.campushub.support.service.SupportCoursService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component("supportSecurity")
public class SupportSecurity {

    private final SupportCoursService supportCoursService;
    private final UserServiceClient userServiceClient;

    public SupportSecurity(SupportCoursService supportCoursService, UserServiceClient userServiceClient) {
        this.supportCoursService = supportCoursService;
        this.userServiceClient = userServiceClient;
    }

    public boolean isOwner(Authentication authentication, Long supportId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        Long authenticatedUserId = userServiceClient.getUserIdByUsername(username);

        if (authenticatedUserId == null) {
            return false; // Could not retrieve user ID from user-service
        }

        return supportCoursService.getSupportById(supportId)
                .map(supportCours -> supportCours.getEnseignantId().equals(authenticatedUserId))
                .orElse(false);
    }

    public boolean isUser(Authentication authentication, Long pathUserId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String username = ((UserDetails) authentication.getPrincipal()).getUsername();
        Long authenticatedUserId = userServiceClient.getUserIdByUsername(username);

        return authenticatedUserId != null && authenticatedUserId.equals(pathUserId);
    }
}
