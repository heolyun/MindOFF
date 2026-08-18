package com.mindoff.api.web;

import com.mindoff.api.domain.AppUser;
import com.mindoff.api.domain.HouseholdMember;
import com.mindoff.api.service.AccessService;
import com.mindoff.api.service.HouseholdInvitationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/household-invitations")
public class HouseholdInvitationController {
    private final AccessService accessService;
    private final HouseholdInvitationService invitationService;

    public HouseholdInvitationController(
            AccessService accessService,
            HouseholdInvitationService invitationService
    ) {
        this.accessService = accessService;
        this.invitationService = invitationService;
    }

    @PostMapping("/{token}/accept")
    public HouseholdController.MemberResponse accept(
            @PathVariable String token,
            @Valid @RequestBody AcceptInvitationRequest request
    ) {
        HouseholdMember member = invitationService.accept(token, request.userId());
        AppUser user = accessService.requireUser(member.getUserId());
        return new HouseholdController.MemberResponse(user.getId(), user.getEmail(), user.getName(), member.getRole());
    }

    public record AcceptInvitationRequest(@NotNull UUID userId) {
    }
}
