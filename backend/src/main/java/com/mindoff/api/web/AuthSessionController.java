package com.mindoff.api.web;

import com.mindoff.api.domain.AppUser;
import com.mindoff.api.domain.Household;
import com.mindoff.api.service.BootstrapService;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@ConditionalOnProperty(name = "mindoff.security.mode", havingValue = "cognito")
public class AuthSessionController {
    private final BootstrapService bootstrapService;

    public AuthSessionController(BootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @PostMapping("/session")
    public SessionResponse session(@AuthenticationPrincipal Jwt jwt) {
        String username = firstNonBlank(jwt.getClaimAsString("cognito:username"), jwt.getSubject());
        String email = firstNonBlank(jwt.getClaimAsString("email"), jwt.getSubject() + "@cognito.local");
        String name = firstNonBlank(jwt.getClaimAsString("name"), username);
        BootstrapService.BootstrapResult result = bootstrapService.bootstrapCognito(jwt.getSubject(), email, name);
        return SessionResponse.from(result.user(), result.household());
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) return value;
        }
        return "MindOFF User";
    }

    public record SessionResponse(
            UUID userId,
            String email,
            String userName,
            UUID householdId,
            String householdName
    ) {
        static SessionResponse from(AppUser user, Household household) {
            return new SessionResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    household.getId(),
                    household.getName()
            );
        }
    }
}
