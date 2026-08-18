package com.mindoff.api.web;

import com.mindoff.api.domain.AppUser;
import com.mindoff.api.domain.Household;
import com.mindoff.api.service.BootstrapService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev")
public class DevBootstrapController {
    private final BootstrapService bootstrapService;

    public DevBootstrapController(BootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @PostMapping("/bootstrap")
    public BootstrapResponse bootstrap(@Valid @RequestBody BootstrapRequest request) {
        BootstrapService.BootstrapResult result = bootstrapService.bootstrap(
                request.email(),
                request.name(),
                request.householdName()
        );
        return BootstrapResponse.from(result.user(), result.household());
    }

    public record BootstrapRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Size(max = 120) String householdName
    ) {
    }

    public record BootstrapResponse(
            UUID userId,
            String email,
            String userName,
            UUID householdId,
            String householdName
    ) {
        static BootstrapResponse from(AppUser user, Household household) {
            return new BootstrapResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    household.getId(),
                    household.getName()
            );
        }
    }
}
