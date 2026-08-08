package com.app.fooddonation.controller.api;

import com.app.fooddonation.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/stats")
@Tag(name = "Statistics", description = "Platform-wide impact metrics")
public class StatsApiController {

    private final StatsService statsService;

    public StatsApiController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/platform")
    @Operation(summary = "Aggregated platform impact statistics (cached)")
    public ResponseEntity<Map<String, Object>> platformStats() {
        return ResponseEntity.ok(statsService.getPlatformStats());
    }
}
