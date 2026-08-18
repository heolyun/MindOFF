package com.mindoff.api.web;

import com.mindoff.api.domain.NeedListItem;
import com.mindoff.api.domain.NeedSourceType;
import com.mindoff.api.repository.NeedListItemRepository;
import com.mindoff.api.service.AccessService;
import com.mindoff.api.service.NeedListService;
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
public class NeedListController {
    private final AccessService accessService;
    private final NeedListItemRepository needListRepository;
    private final NeedListService needListService;

    public NeedListController(
            AccessService accessService,
            NeedListItemRepository needListRepository,
            NeedListService needListService
    ) {
        this.accessService = accessService;
        this.needListRepository = needListRepository;
        this.needListService = needListService;
    }

    @GetMapping("/households/{householdId}/needs")
    @Transactional(readOnly = true)
    public List<NeedListItem> list(@PathVariable UUID householdId, @RequestParam UUID userId) {
        accessService.requireMember(householdId, userId);
        return needListRepository.findAllByHouseholdIdOrderByCreatedAtDesc(householdId);
    }

    @PostMapping("/households/{householdId}/needs")
    @Transactional
    public NeedListItem add(
            @PathVariable UUID householdId,
            @Valid @RequestBody AddNeedRequest request
    ) {
        accessService.requireMember(householdId, request.userId());
        return needListRepository.save(new NeedListItem(
                householdId,
                NeedSourceType.MANUAL,
                null,
                request.name(),
                request.purchaseUrl()
        ));
    }

    @PatchMapping("/needs/{itemId}/complete")
    public NeedListItem complete(
            @PathVariable UUID itemId,
            @Valid @RequestBody CompleteNeedRequest request
    ) {
        return needListService.complete(itemId, request.userId(), request.purchasedAt());
    }

    public record AddNeedRequest(
            @NotNull UUID userId,
            @NotBlank @Size(max = 160) String name,
            @Size(max = 1000) String purchaseUrl
    ) {
    }

    public record CompleteNeedRequest(@NotNull UUID userId, LocalDate purchasedAt) {
    }
}
