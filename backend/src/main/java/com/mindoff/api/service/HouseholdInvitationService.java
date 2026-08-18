package com.mindoff.api.service;

import com.mindoff.api.domain.AppUser;
import com.mindoff.api.domain.HouseholdInvitation;
import com.mindoff.api.domain.HouseholdInvitationStatus;
import com.mindoff.api.domain.HouseholdMember;
import com.mindoff.api.domain.HouseholdRole;
import com.mindoff.api.repository.HouseholdInvitationRepository;
import com.mindoff.api.repository.HouseholdMemberRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HouseholdInvitationService {
    private final AccessService accessService;
    private final HouseholdInvitationRepository invitationRepository;
    private final HouseholdMemberRepository memberRepository;

    public HouseholdInvitationService(
            AccessService accessService,
            HouseholdInvitationRepository invitationRepository,
            HouseholdMemberRepository memberRepository
    ) {
        this.accessService = accessService;
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
    }

    @Transactional
    public HouseholdInvitation create(UUID householdId, UUID requesterId, String email) {
        accessService.requireOwner(householdId, requesterId);
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        Instant now = Instant.now();
        HouseholdInvitation existing = invitationRepository
                .findFirstByHouseholdIdAndEmailIgnoreCaseAndStatusOrderByCreatedAtDesc(
                        householdId,
                        normalizedEmail,
                        HouseholdInvitationStatus.PENDING
                )
                .orElse(null);
        if (existing != null && !existing.isExpired(now)) {
            return existing;
        }
        if (existing != null) {
            existing.expire();
        }
        return invitationRepository.save(new HouseholdInvitation(
                householdId,
                normalizedEmail,
                requesterId,
                now.plus(7, ChronoUnit.DAYS)
        ));
    }

    @Transactional(readOnly = true)
    public List<HouseholdInvitation> list(UUID householdId, UUID requesterId) {
        accessService.requireOwner(householdId, requesterId);
        return invitationRepository.findAllByHouseholdIdOrderByCreatedAtDesc(householdId);
    }

    @Transactional
    public HouseholdMember accept(String token, UUID userId) {
        AppUser user = accessService.requireUser(userId);
        HouseholdInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "초대를 찾을 수 없습니다."));
        Instant now = Instant.now();
        if (invitation.getStatus() == HouseholdInvitationStatus.ACCEPTED) {
            return memberRepository.findByHouseholdIdAndUserId(invitation.getHouseholdId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "초대 상태를 확인할 수 없습니다."));
        }
        if (invitation.getStatus() != HouseholdInvitationStatus.PENDING || invitation.isExpired(now)) {
            if (invitation.isExpired(now)) invitation.expire();
            throw new ResponseStatusException(HttpStatus.GONE, "초대가 만료되었습니다.");
        }
        if (!invitation.getEmail().equalsIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "초대받은 이메일로 로그인해야 합니다.");
        }
        HouseholdMember member = memberRepository.findByHouseholdIdAndUserId(invitation.getHouseholdId(), userId)
                .orElseGet(() -> memberRepository.save(new HouseholdMember(
                        invitation.getHouseholdId(),
                        userId,
                        HouseholdRole.MEMBER
                )));
        invitation.accept(now);
        return member;
    }
}
