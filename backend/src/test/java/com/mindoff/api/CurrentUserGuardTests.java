package com.mindoff.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mindoff.api.domain.AppUser;
import com.mindoff.api.repository.AppUserRepository;
import com.mindoff.api.service.CurrentUserGuard;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

class CurrentUserGuardTests {
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void cognitoModeAllowsOnlyTheUserLinkedToJwtSubject() {
        AppUserRepository repository = mock(AppUserRepository.class);
        AppUser user = AppUser.cognito("subject-1", "member@mindoff.local", "Member");
        when(repository.findByExternalSubject("subject-1")).thenReturn(Optional.of(user));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("subject-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        CurrentUserGuard guard = new CurrentUserGuard("cognito", repository);
        guard.requireCurrentUser(user.getId());

        UUID anotherUser = UUID.randomUUID();
        assertThatThrownBy(() -> guard.requireCurrentUser(anotherUser))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN)
                );
    }
}
