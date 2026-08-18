package com.mindoff.api.service;

import com.mindoff.api.domain.AppUser;
import com.mindoff.api.repository.AppUserRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CurrentUserGuard {
    private final boolean cognitoMode;
    private final AppUserRepository userRepository;

    public CurrentUserGuard(
            @Value("${mindoff.security.mode:dev}") String mode,
            AppUserRepository userRepository
    ) {
        this.cognitoMode = "cognito".equalsIgnoreCase(mode);
        this.userRepository = userRepository;
    }

    public void requireCurrentUser(UUID userId) {
        if (!cognitoMode) {
            return;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        AppUser authenticatedUser = userRepository.findByExternalSubject(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용자 세션을 먼저 생성하세요."));
        if (!authenticatedUser.getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "다른 사용자의 데이터에는 접근할 수 없습니다.");
        }
    }
}
