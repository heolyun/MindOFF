package com.mindoff.api.service;

import com.mindoff.api.domain.FridgeStatus;
import com.mindoff.api.domain.HouseholdItemStatus;
import com.mindoff.api.repository.FridgeItemRepository;
import com.mindoff.api.repository.HouseholdItemRepository;
import com.mindoff.api.repository.SubscriptionRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttentionService {
    private final AccessService accessService;
    private final FridgeItemRepository fridgeRepository;
    private final HouseholdItemRepository householdItemRepository;
    private final SubscriptionRepository subscriptionRepository;

    public AttentionService(
            AccessService accessService,
            FridgeItemRepository fridgeRepository,
            HouseholdItemRepository householdItemRepository,
            SubscriptionRepository subscriptionRepository
    ) {
        this.accessService = accessService;
        this.fridgeRepository = fridgeRepository;
        this.householdItemRepository = householdItemRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional(readOnly = true)
    public List<AttentionItem> list(UUID householdId, UUID userId) {
        accessService.requireMember(householdId, userId);
        LocalDate today = LocalDate.now();
        LocalDate limit = today.plusDays(2);
        List<AttentionItem> result = new ArrayList<>();

        fridgeRepository.findAllByHouseholdIdOrderByExpiresAtAsc(householdId).stream()
                .filter(item -> item.getStatus() == FridgeStatus.ACTIVE)
                .filter(item -> item.getExpiresAt() != null && !item.getExpiresAt().isAfter(limit))
                .forEach(item -> result.add(new AttentionItem(
                        "FRIDGE_EXPIRY",
                        item.getId(),
                        item.getExpiresAt(),
                        item.getName(),
                        item.getExpiresAt().isBefore(today) ? "기한 지남" : "곧 만료"
                )));

        householdItemRepository.findAllByHouseholdIdOrderByCreatedAtDesc(householdId).stream()
                .filter(item -> item.getStatus() == HouseholdItemStatus.ACTIVE)
                .filter(item -> item.getPredictedDays() != null)
                .map(item -> new PredictedItem(item.getId(), item.getName(), item.getPurchasedAt().plusDays(item.getPredictedDays())))
                .filter(item -> !item.dueAt().isAfter(limit))
                .forEach(item -> result.add(new AttentionItem(
                        "USAGE_PREDICTION",
                        item.id(),
                        item.dueAt(),
                        item.name(),
                        "재구매 시점"
                )));

        subscriptionRepository.findAllByUserIdOrderByNextBillingAtAsc(userId).stream()
                .filter(item -> item.getTrialEndAt() != null)
                .filter(item -> !item.getTrialEndAt().isBefore(today) && !item.getTrialEndAt().isAfter(limit))
                .forEach(item -> result.add(new AttentionItem(
                        "TRIAL_END",
                        item.getId(),
                        item.getTrialEndAt(),
                        item.getName(),
                        "체험 종료"
                )));

        result.sort(Comparator.comparing(AttentionItem::dueAt));
        return result;
    }

    private record PredictedItem(UUID id, String name, LocalDate dueAt) {
    }

    public record AttentionItem(String type, UUID sourceId, LocalDate dueAt, String title, String message) {
        public long daysFromToday() {
            return ChronoUnit.DAYS.between(LocalDate.now(), dueAt);
        }
    }
}
