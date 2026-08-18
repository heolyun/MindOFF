package com.mindoff.api.web;

import com.mindoff.api.service.HomeService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
public class HomeController {
    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping
    public HomeService.HomeSummary get(
            @RequestParam UUID householdId,
            @RequestParam UUID userId
    ) {
        return homeService.summarize(householdId, userId);
    }
}
