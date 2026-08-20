package com.mindoff.api.service;

import com.mindoff.api.domain.BillingCycle;
import com.mindoff.api.domain.NeedListStatus;
import com.mindoff.api.domain.Subscription;
import com.mindoff.api.domain.ReceiptStatus;
import com.mindoff.api.repository.NeedListItemRepository;
import com.mindoff.api.repository.SubscriptionRepository;
import com.mindoff.api.repository.ReceiptRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeService {
    private final AccessService accessService;
    private final NeedListItemRepository needListRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ReceiptRepository receiptRepository;
    private final AttentionService attentionService;

    public HomeService(
            AccessService accessService,
            NeedListItemRepository needListRepository,
            SubscriptionRepository subscriptionRepository,
            ReceiptRepository receiptRepository,
            AttentionService attentionService
    ) {
        this.accessService = accessService;
        this.needListRepository = needListRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.receiptRepository = receiptRepository;
        this.attentionService = attentionService;
    }

    @Transactional(readOnly = true)
    public HomeSummary summarize(UUID householdId, UUID userId) {
        accessService.requireMember(householdId, userId);
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = today.withDayOfMonth(today.lengthOfMonth());
        List<Subscription> subscriptions = subscriptionRepository.findVisibleToUser(userId, householdId);
        BigDecimal monthlyFixedCost = subscriptions.stream()
                .map(HomeService::monthlyAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(0, RoundingMode.HALF_UP);

        long needCount = needListRepository.countByHouseholdIdAndStatus(householdId, NeedListStatus.NEEDED);
        BigDecimal receiptTotal = receiptRepository.findAllByHouseholdIdAndStatusAndPurchasedAtBetween(
                        householdId,
                        ReceiptStatus.CONFIRMED,
                        monthStart,
                        monthEnd
                )
                .stream()
                .map(receipt -> receipt.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new HomeSummary(
                attentionService.list(householdId, userId).size(),
                needCount,
                monthlyFixedCost,
                receiptTotal
        );
    }

    private static BigDecimal monthlyAmount(Subscription subscription) {
        if (subscription.getBillingCycle() == BillingCycle.ANNUAL) {
            return subscription.getAmount().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        }
        return subscription.getAmount();
    }

    public record HomeSummary(
            long attentionCount,
            long needListCount,
            BigDecimal recordedFixedLivingCost,
            BigDecimal receiptPurchaseTotal
    ) {
    }
}
