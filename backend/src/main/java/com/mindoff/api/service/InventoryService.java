package com.mindoff.api.service;

import com.mindoff.api.domain.FridgeItem;
import com.mindoff.api.domain.FridgeStatus;
import com.mindoff.api.domain.HouseholdItem;
import com.mindoff.api.domain.HouseholdItemStatus;
import com.mindoff.api.domain.NeedListItem;
import com.mindoff.api.domain.NeedSourceType;
import com.mindoff.api.domain.UsageCycle;
import com.mindoff.api.repository.FridgeItemRepository;
import com.mindoff.api.repository.HouseholdItemRepository;
import com.mindoff.api.repository.NeedListItemRepository;
import com.mindoff.api.repository.UsageCycleRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventoryService {
    private final AccessService accessService;
    private final FridgeItemRepository fridgeRepository;
    private final HouseholdItemRepository householdItemRepository;
    private final UsageCycleRepository usageCycleRepository;
    private final NeedListItemRepository needListRepository;

    public InventoryService(
            AccessService accessService,
            FridgeItemRepository fridgeRepository,
            HouseholdItemRepository householdItemRepository,
            UsageCycleRepository usageCycleRepository,
            NeedListItemRepository needListRepository
    ) {
        this.accessService = accessService;
        this.fridgeRepository = fridgeRepository;
        this.householdItemRepository = householdItemRepository;
        this.usageCycleRepository = usageCycleRepository;
        this.needListRepository = needListRepository;
    }

    @Transactional
    public FridgeItem addFridgeItem(
            UUID householdId,
            UUID userId,
            String name,
            LocalDate purchasedAt,
            LocalDate expiresAt
    ) {
        accessService.requireMember(householdId, userId);
        return fridgeRepository.save(new FridgeItem(householdId, name, purchasedAt, expiresAt));
    }

    @Transactional
    public HouseholdItem addHouseholdItem(
            UUID householdId,
            UUID userId,
            String name,
            LocalDate purchasedAt,
            boolean repeatPurchase,
            String purchaseUrl
    ) {
        accessService.requireMember(householdId, userId);
        HouseholdItem item = new HouseholdItem(householdId, name, purchasedAt, repeatPurchase, purchaseUrl);
        List<HouseholdItem> history = householdItemRepository
                .findTop3ByHouseholdIdAndNameIgnoreCaseAndStatusOrderByFinishedAtDesc(
                        householdId,
                        name.trim(),
                        HouseholdItemStatus.FINISHED
                );
        if (!history.isEmpty()) {
            int weightedTotal = 0;
            int weightTotal = 0;
            int weight = history.size();
            for (HouseholdItem previous : history) {
                if (previous.getPredictedDays() != null) {
                    weightedTotal += previous.getPredictedDays() * weight;
                    weightTotal += weight;
                }
                weight--;
            }
            if (weightTotal > 0) {
                item.applyPrediction(Math.max(1, Math.round((float) weightedTotal / weightTotal)));
            }
        }
        return householdItemRepository.save(item);
    }

    @Transactional
    public FridgeItem finishFridgeItem(UUID itemId, UUID userId, boolean addToNeedList) {
        FridgeItem item = fridgeRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "냉장고 품목을 찾을 수 없습니다."));
        accessService.requireMember(item.getHouseholdId(), userId);
        if (item.getStatus() == FridgeStatus.FINISHED) {
            return item;
        }
        item.finish();
        if (addToNeedList) {
            needListRepository.save(new NeedListItem(
                    item.getHouseholdId(),
                    NeedSourceType.FRIDGE,
                    item.getId(),
                    item.getName(),
                    null
            ));
        }
        return item;
    }

    @Transactional
    public HouseholdItem finishHouseholdItem(UUID itemId, UUID userId, boolean addToNeedList) {
        HouseholdItem item = householdItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "생활용품을 찾을 수 없습니다."));
        accessService.requireMember(item.getHouseholdId(), userId);
        if (item.getStatus() == HouseholdItemStatus.FINISHED) {
            return item;
        }

        LocalDate finishedAt = LocalDate.now();
        int durationDays = Math.max(1, Math.toIntExact(ChronoUnit.DAYS.between(item.getPurchasedAt(), finishedAt)));
        usageCycleRepository.save(new UsageCycle(item.getId(), item.getPurchasedAt(), finishedAt, durationDays));
        item.finish(finishedAt, durationDays);

        if (addToNeedList || item.isRepeatPurchase()) {
            needListRepository.save(new NeedListItem(
                    item.getHouseholdId(),
                    NeedSourceType.HOUSEHOLD_ITEM,
                    item.getId(),
                    item.getName(),
                    item.getPurchaseUrl()
            ));
        }
        return item;
    }

    @Transactional
    public HouseholdItem keepUsingHouseholdItem(UUID itemId, UUID userId) {
        HouseholdItem item = householdItemRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "생활용품을 찾을 수 없습니다."));
        accessService.requireMember(item.getHouseholdId(), userId);
        if (item.getStatus() != HouseholdItemStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "사용 중인 생활용품만 보정할 수 있습니다.");
        }
        int elapsedDays = Math.max(1, Math.toIntExact(ChronoUnit.DAYS.between(item.getPurchasedAt(), LocalDate.now())));
        item.applyPrediction(elapsedDays + 7);
        return item;
    }

    @Transactional
    public HouseholdItem repurchaseHouseholdItem(UUID previousItemId, UUID userId, LocalDate purchasedAt) {
        return repurchaseHouseholdItem(previousItemId, userId, purchasedAt, null, null);
    }

    @Transactional
    public HouseholdItem repurchaseHouseholdItem(
            UUID previousItemId,
            UUID userId,
            LocalDate purchasedAt,
            String name,
            String purchaseUrl
    ) {
        HouseholdItem previous = householdItemRepository.findById(previousItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이전 생활용품을 찾을 수 없습니다."));
        accessService.requireMember(previous.getHouseholdId(), userId);
        HouseholdItem next = addHouseholdItem(
                previous.getHouseholdId(),
                userId,
                name == null || name.isBlank() ? previous.getName() : name,
                purchasedAt,
                previous.isRepeatPurchase(),
                purchaseUrl == null ? previous.getPurchaseUrl() : purchaseUrl
        );
        if (next.getPredictedDays() == null && previous.getPredictedDays() != null) {
            next.applyPrediction(previous.getPredictedDays());
        }
        return next;
    }

    @Transactional
    public FridgeItem repurchaseFridgeItem(
            UUID previousItemId,
            UUID userId,
            LocalDate purchasedAt,
            LocalDate expiresAt,
            String name
    ) {
        FridgeItem previous = fridgeRepository.findById(previousItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "이전 냉장고 품목을 찾을 수 없습니다."));
        accessService.requireMember(previous.getHouseholdId(), userId);
        return addFridgeItem(
                previous.getHouseholdId(),
                userId,
                name == null || name.isBlank() ? previous.getName() : name,
                purchasedAt,
                expiresAt
        );
    }
}
