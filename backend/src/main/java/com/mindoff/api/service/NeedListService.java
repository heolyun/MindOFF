package com.mindoff.api.service;

import com.mindoff.api.domain.NeedListItem;
import com.mindoff.api.domain.NeedListStatus;
import com.mindoff.api.domain.NeedSourceType;
import com.mindoff.api.repository.NeedListItemRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class NeedListService {
    private final AccessService accessService;
    private final InventoryService inventoryService;
    private final NeedListItemRepository needListRepository;

    public NeedListService(
            AccessService accessService,
            InventoryService inventoryService,
            NeedListItemRepository needListRepository
    ) {
        this.accessService = accessService;
        this.inventoryService = inventoryService;
        this.needListRepository = needListRepository;
    }

    @Transactional
    public NeedListItem complete(
            UUID itemId,
            UUID userId,
            LocalDate purchasedAt,
            LocalDate expiresAt,
            String name,
            String purchaseUrl
    ) {
        NeedListItem item = needListRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "구매목록 품목을 찾을 수 없습니다."));
        accessService.requireMember(item.getHouseholdId(), userId);
        if (item.getStatus() != NeedListStatus.NEEDED) {
            return item;
        }
        if (item.getSourceType() == NeedSourceType.HOUSEHOLD_ITEM && item.getSourceId() != null) {
            inventoryService.repurchaseHouseholdItem(
                    item.getSourceId(),
                    userId,
                    purchasedAt == null ? LocalDate.now() : purchasedAt,
                    name,
                    purchaseUrl
            );
        }
        if (item.getSourceType() == NeedSourceType.FRIDGE && item.getSourceId() != null) {
            inventoryService.repurchaseFridgeItem(
                    item.getSourceId(),
                    userId,
                    purchasedAt == null ? LocalDate.now() : purchasedAt,
                    expiresAt,
                    name
            );
        }
        item.complete();
        return item;
    }
}
