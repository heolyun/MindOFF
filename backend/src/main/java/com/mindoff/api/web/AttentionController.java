package com.mindoff.api.web;

import com.mindoff.api.service.AttentionService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/attention")
public class AttentionController {
    private final AttentionService attentionService;

    public AttentionController(AttentionService attentionService) {
        this.attentionService = attentionService;
    }

    @GetMapping
    public List<AttentionService.AttentionItem> list(
            @RequestParam UUID householdId,
            @RequestParam UUID userId
    ) {
        return attentionService.list(householdId, userId);
    }
}
