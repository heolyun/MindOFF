package com.mindoff.api.web;

import com.mindoff.api.domain.HouseholdItem;
import com.mindoff.api.repository.HouseholdItemRepository;
import com.mindoff.api.service.AccessService;
import com.mindoff.api.service.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HouseholdItemController {
    private final AccessService accessService;
    private final HouseholdItemRepository itemRepository;
    private final InventoryService inventoryService;

    public HouseholdItemController(
            AccessService accessService,
            HouseholdItemRepository itemRepository,
            InventoryService inventoryService
    ) {
        this.accessService = accessService;
        this.itemRepository = itemRepository;
        this.inventoryService = inventoryService;
    }

    @GetMapping("/households/{householdId}/items")
    @Transactional(readOnly = true)
    public List<HouseholdItem> list(@PathVariable UUID householdId, @RequestParam UUID userId) {
        accessService.requireMember(householdId, userId);
        return itemRepository.findAllByHouseholdIdOrderByCreatedAtDesc(householdId);
    }

    @PostMapping("/households/{householdId}/items")
    @Transactional
    public HouseholdItem add(
            @PathVariable UUID householdId,
            @Valid @RequestBody AddHouseholdItemRequest request
    ) {
        return inventoryService.addHouseholdItem(
                householdId,
                request.userId(),
                request.name(),
                request.purchasedAt(),
                request.repeatPurchase(),
                request.purchaseUrl()
        );
    }

    @PatchMapping("/household-items/{itemId}/finish")
    public HouseholdItem finish(
            @PathVariable UUID itemId,
            @Valid @RequestBody FinishRequest request
    ) {
        return inventoryService.finishHouseholdItem(itemId, request.userId(), request.addToNeedList());
    }

    @PatchMapping("/household-items/{itemId}/still-using")
    public HouseholdItem keepUsing(
            @PathVariable UUID itemId,
            @Valid @RequestBody StillUsingRequest request
    ) {
        return inventoryService.keepUsingHouseholdItem(itemId, request.userId());
    }

    public record AddHouseholdItemRequest(
            @NotNull UUID userId,
            @NotBlank @Size(max = 160) String name,
            @NotNull LocalDate purchasedAt,
            boolean repeatPurchase,
            @Size(max = 1000) String purchaseUrl
    ) {
    }

    public record FinishRequest(@NotNull UUID userId, boolean addToNeedList) {
    }

    public record StillUsingRequest(@NotNull UUID userId) {
    }
}
