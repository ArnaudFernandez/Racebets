package com.pixsom.racebets.realtime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/realtime")
public class RealtimeRaceBettingController {

    private final RealtimeRaceBettingService realtimeRaceBettingService;

    public RealtimeRaceBettingController(RealtimeRaceBettingService realtimeRaceBettingService) {
        this.realtimeRaceBettingService = realtimeRaceBettingService;
    }

    @GetMapping("/race-betting")
    public List<RaceBettingUpdateResponse> raceBetting() {
        return realtimeRaceBettingService.currentSnapshots();
    }
}
