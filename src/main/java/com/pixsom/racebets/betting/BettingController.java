package com.pixsom.racebets.betting;

import com.pixsom.racebets.betting.dto.LiveRaceResponse;
import com.pixsom.racebets.betting.dto.PlaceBetRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/betting")
public class BettingController {

    private final BettingService bettingService;

    public BettingController(BettingService bettingService) {
        this.bettingService = bettingService;
    }

    @GetMapping("/live")
    public ResponseEntity<LiveRaceResponse> live(@AuthenticationPrincipal Jwt jwt) {
        return bettingService.currentRace(userId(jwt))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/races/{raceId}/bets")
    public LiveRaceResponse placeBet(@PathVariable Long raceId, @Valid @RequestBody PlaceBetRequest request,
                                     @AuthenticationPrincipal Jwt jwt) {
        return bettingService.placeBet(raceId, request.raceEntryId(), userId(jwt));
    }

    private Long userId(Jwt jwt) {
        return jwt.getClaim("userId");
    }
}
