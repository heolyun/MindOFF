package com.mindoff.api.web;

import com.mindoff.api.domain.AppUser;
import com.mindoff.api.domain.Household;
import com.mindoff.api.domain.HouseholdInvitation;
import com.mindoff.api.domain.HouseholdInvitationStatus;
import com.mindoff.api.domain.HouseholdMember;
import com.mindoff.api.domain.HouseholdRole;
import com.mindoff.api.repository.HouseholdMemberRepository;
import com.mindoff.api.service.AccessService;
import com.mindoff.api.service.HouseholdInvitationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/households")
public class HouseholdController {
    private final AccessService accessService;
    private final HouseholdMemberRepository memberRepository;
    private final HouseholdInvitationService invitationService;

    public HouseholdController(
            AccessService accessService,
            HouseholdMemberRepository memberRepository,
            HouseholdInvitationService invitationService
    ) {
        this.accessService = accessService;
        this.memberRepository = memberRepository;
        this.invitationService = invitationService;
    }

    @GetMapping("/{householdId}")
    @Transactional(readOnly = true)
    public HouseholdResponse get(
            @PathVariable UUID householdId,
            @RequestParam UUID userId
    ) {
        accessService.requireMember(householdId, userId);
        Household household = accessService.requireHousehold(householdId);
        List<MemberResponse> members = memberRepository.findAllByHouseholdIdOrderByCreatedAtAsc(householdId)
                .stream()
                .map(member -> MemberResponse.from(member, accessService.requireUser(member.getUserId())))
                .toList();
        return new HouseholdResponse(household.getId(), household.getName(), household.getOwnerId(), members);
    }

    @PostMapping("/{householdId}/invitations")
    public InvitationResponse createInvitation(
            @PathVariable UUID householdId,
            @Valid @RequestBody InviteRequest request
    ) {
        return InvitationResponse.from(invitationService.create(householdId, request.requesterId(), request.email()));
    }

    @GetMapping("/{householdId}/invitations")
    public List<InvitationResponse> listInvitations(
            @PathVariable UUID householdId,
            @RequestParam UUID userId
    ) {
        return invitationService.list(householdId, userId).stream()
                .map(InvitationResponse::from)
                .toList();
    }

    public record InviteRequest(@NotNull UUID requesterId, @NotNull @Email String email) {
    }

    public record HouseholdResponse(UUID id, String name, UUID ownerId, List<MemberResponse> members) {
    }

    public record MemberResponse(UUID userId, String email, String name, HouseholdRole role) {
        static MemberResponse from(HouseholdMember member, AppUser user) {
            return new MemberResponse(user.getId(), user.getEmail(), user.getName(), member.getRole());
        }
    }

    public record InvitationResponse(
            UUID id,
            UUID householdId,
            String email,
            String token,
            HouseholdInvitationStatus status,
            Instant expiresAt,
            Instant createdAt,
            Instant acceptedAt
    ) {
        static InvitationResponse from(HouseholdInvitation invitation) {
            return new InvitationResponse(
                    invitation.getId(),
                    invitation.getHouseholdId(),
                    invitation.getEmail(),
                    invitation.getToken(),
                    invitation.getStatus(),
                    invitation.getExpiresAt(),
                    invitation.getCreatedAt(),
                    invitation.getAcceptedAt()
            );
        }
    }
}
