package com.mindoff.api.web;

import com.mindoff.api.domain.FridgeItem;
import com.mindoff.api.repository.FridgeItemRepository;
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
public class FridgeController {
    private final AccessService accessService;
    private final FridgeItemRepository fridgeRepository;
    private final InventoryService inventoryService;

    public FridgeController(
            AccessService accessService,
            FridgeItemRepository fridgeRepository,
            InventoryService inventoryService
    ) {
        this.accessService = accessService;
        this.fridgeRepository = fridgeRepository;
        this.inventoryService = inventoryService;
    }

    @GetMapping("/households/{householdId}/fridge")
    @Transactional(readOnly = true)
    public List<FridgeItem> list(@PathVariable UUID householdId, @RequestParam UUID userId) {
        accessService.requireMember(householdId, userId);
        return fridgeRepository.findAllByHouseholdIdOrderByExpiresAtAsc(householdId);
    }

    @PostMapping("/households/{householdId}/fridge")
    @Transactional
    public FridgeItem add(
            @PathVariable UUID householdId,
            @Valid @RequestBody AddFridgeItemRequest request
    ) {
        return inventoryService.addFridgeItem(
                householdId,
                request.userId(),
                request.name(),
                request.purchasedAt(),
                request.expiresAt()
        );
    }

    @PatchMapping("/fridge/{itemId}/finish")
    public FridgeItem finish(
            @PathVariable UUID itemId,
            @Valid @RequestBody FinishRequest request
    ) {
        return inventoryService.finishFridgeItem(itemId, request.userId(), request.addToNeedList());
    }

    public record AddFridgeItemRequest(
            @NotNull UUID userId,
            @NotBlank @Size(max = 160) String name,
            @NotNull LocalDate purchasedAt,
            LocalDate expiresAt
    ) {
    }

    public record FinishRequest(@NotNull UUID userId, boolean addToNeedList) {
    }
}
