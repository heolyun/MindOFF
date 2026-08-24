package com.mindoff.api.service;

import com.mindoff.api.domain.BillingCycle;
import com.mindoff.api.domain.Subscription;
import com.mindoff.api.repository.SubscriptionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SubscriptionService {
    private final AccessService accessService;
    private final SubscriptionRepository subscriptionRepository;

    public SubscriptionService(AccessService accessService, SubscriptionRepository subscriptionRepository) {
        this.accessService = accessService;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public List<Subscription> list(UUID userId, UUID householdId) {
        accessService.requireMember(householdId, userId);
        return subscriptionRepository.findVisibleToUser(userId, householdId);
    }

    @Transactional
    public Subscription add(UUID userId, SubscriptionInput input) {
        requireAccess(userId, input);
        return subscriptionRepository.save(new Subscription(
                userId,
                input.name(),
                input.amount(),
                input.billingCycle(),
                input.nextBillingAt(),
                input.trialEndAt(),
                input.managementUrl(),
                input.shared(),
                input.householdId()
        ));
    }

    @Transactional
    public Subscription update(UUID userId, UUID subscriptionId, SubscriptionInput input) {
        requireAccess(userId, input);
        Subscription subscription = ownedSubscription(userId, subscriptionId);
        subscription.update(
                input.name(),
                input.amount(),
                input.billingCycle(),
                input.nextBillingAt(),
                input.trialEndAt(),
                input.managementUrl(),
                input.shared(),
                input.householdId()
        );
        return subscription;
    }

    @Transactional
    public void delete(UUID userId, UUID subscriptionId) {
        accessService.requireUser(userId);
        subscriptionRepository.delete(ownedSubscription(userId, subscriptionId));
    }

    private void requireAccess(UUID userId, SubscriptionInput input) {
        if (input.shared()) {
            accessService.requireMember(input.householdId(), userId);
        } else {
            accessService.requireUser(userId);
        }
    }

    private Subscription ownedSubscription(UUID userId, UUID subscriptionId) {
        return subscriptionRepository.findByIdAndUserId(subscriptionId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "구독을 찾을 수 없습니다."));
    }

    public record SubscriptionInput(
            String name,
            BigDecimal amount,
            BillingCycle billingCycle,
            LocalDate nextBillingAt,
            LocalDate trialEndAt,
            String managementUrl,
            boolean shared,
            UUID householdId
    ) {
    }
}
