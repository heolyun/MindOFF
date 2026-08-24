package com.mindoff.api.web;

import com.mindoff.api.domain.BillingCycle;
import com.mindoff.api.domain.Subscription;
import com.mindoff.api.service.SubscriptionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping("/api/users/{userId}/subscriptions")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public List<Subscription> list(@PathVariable UUID userId, @RequestParam UUID householdId) {
        return subscriptionService.list(userId, householdId);
    }

    @PostMapping
    public Subscription add(
            @PathVariable UUID userId,
            @Valid @RequestBody AddSubscriptionRequest request
    ) {
        return subscriptionService.add(userId, request.toInput());
    }

    @PatchMapping("/{subscriptionId}")
    public Subscription update(
            @PathVariable UUID userId,
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody AddSubscriptionRequest request
    ) {
        return subscriptionService.update(userId, subscriptionId, request.toInput());
    }

    @DeleteMapping("/{subscriptionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID userId, @PathVariable UUID subscriptionId) {
        subscriptionService.delete(userId, subscriptionId);
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
        SubscriptionService.SubscriptionInput toInput() {
            return new SubscriptionService.SubscriptionInput(
                    name,
                    amount,
                    billingCycle,
                    nextBillingAt,
                    trialEndAt,
                    managementUrl,
                    shared,
                    householdId
            );
        }
    }
}
