package com.mindoff.api.service;

import com.mindoff.api.domain.AppUser;
import com.mindoff.api.domain.Household;
import com.mindoff.api.domain.HouseholdMember;
import com.mindoff.api.domain.HouseholdRole;
import com.mindoff.api.repository.AppUserRepository;
import com.mindoff.api.repository.HouseholdMemberRepository;
import com.mindoff.api.repository.HouseholdRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessService {
    private final AppUserRepository userRepository;
    private final HouseholdRepository householdRepository;
    private final HouseholdMemberRepository memberRepository;
    private final CurrentUserGuard currentUserGuard;

    public AccessService(
            AppUserRepository userRepository,
            HouseholdRepository householdRepository,
            HouseholdMemberRepository memberRepository,
            CurrentUserGuard currentUserGuard
    ) {
        this.userRepository = userRepository;
        this.householdRepository = householdRepository;
        this.memberRepository = memberRepository;
        this.currentUserGuard = currentUserGuard;
    }

    public AppUser requireUser(UUID userId) {
        currentUserGuard.requireCurrentUser(userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
    }

    public Household requireHousehold(UUID householdId) {
        return householdRepository.findById(householdId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Household를 찾을 수 없습니다."));
    }

    public HouseholdMember requireMember(UUID householdId, UUID userId) {
        currentUserGuard.requireCurrentUser(userId);
        requireHousehold(householdId);
        return memberRepository.findByHouseholdIdAndUserId(householdId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Household 구성원만 접근할 수 있습니다."));
    }

    public HouseholdMember requireOwner(UUID householdId, UUID userId) {
        HouseholdMember member = requireMember(householdId, userId);
        if (member.getRole() != HouseholdRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Household 소유자만 처리할 수 있습니다.");
        }
        return member;
    }
}
