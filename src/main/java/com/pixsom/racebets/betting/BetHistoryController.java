package com.pixsom.racebets.betting;

import com.pixsom.racebets.betting.dto.BetHistoryAvailabilityResponse;
import com.pixsom.racebets.betting.dto.BetHistoryResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/betting/history")
public class BetHistoryController {

    private final BetHistoryService betHistoryService;

    public BetHistoryController(BetHistoryService betHistoryService) {
        this.betHistoryService = betHistoryService;
    }

    @GetMapping
    public List<BetHistoryResponse> findAll(@AuthenticationPrincipal Jwt jwt) {
        return betHistoryService.findAll(userId(jwt));
    }

    @GetMapping("/availability")
    public BetHistoryAvailabilityResponse availability(@AuthenticationPrincipal Jwt jwt) {
        return new BetHistoryAvailabilityResponse(betHistoryService.hasHistory(userId(jwt)));
    }

    private Long userId(Jwt jwt) {
        return jwt.getClaim("userId");
    }
}
