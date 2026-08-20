package com.mindoff.api.web;

import com.mindoff.api.domain.BillingCycle;
import com.mindoff.api.domain.Subscription;
import com.mindoff.api.repository.SubscriptionRepository;
import com.mindoff.api.service.AccessService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
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
@RequestMapping("/api/users/{userId}/subscriptions")
public class SubscriptionController {
    private final AccessService accessService;
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionController(AccessService accessService, SubscriptionRepository subscriptionRepository) {
        this.accessService = accessService;
        this.subscriptionRepository = subscriptionRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Subscription> list(@PathVariable UUID userId, @RequestParam UUID householdId) {
        accessService.requireMember(householdId, userId);
        return subscriptionRepository.findVisibleToUser(userId, householdId);
    }

    @PostMapping
    @Transactional
    public Subscription add(
            @PathVariable UUID userId,
            @Valid @RequestBody AddSubscriptionRequest request
    ) {
        if (request.shared()) {
            accessService.requireMember(request.householdId(), userId);
        } else {
            accessService.requireUser(userId);
        }
        return subscriptionRepository.save(new Subscription(
                userId,
                request.name(),
                request.amount(),
                request.billingCycle(),
                request.nextBillingAt(),
                request.trialEndAt(),
                request.managementUrl(),
                request.shared(),
                request.shared() ? request.householdId() : null
        ));
    }

    public record AddSubscriptionRequest(
            @NotBlank @Size(max = 160) String name,
            @NotNull @DecimalMin("0.00") BigDecimal amount,
            @NotNull BillingCycle billingCycle,
            LocalDate nextBillingAt,
            LocalDate trialEndAt,
            @Size(max = 1000) String managementUrl,
            boolean shared,
            @NotNull UUID householdId
    ) {
    }
}
