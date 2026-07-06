package com.pixsom.racebets.realtime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    @GetMapping(value = "/race-betting/stream", produces = "text/event-stream")
    public SseEmitter streamRaceBetting() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<RaceBettingUpdateResponse> snapshots = realtimeRaceBettingService.currentSnapshots();
                emitter.send(SseEmitter.event()
                        .data(snapshots)
                        .name("message"));
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }, 0, 2, TimeUnit.SECONDS);

        emitter.onCompletion(scheduler::shutdown);
        emitter.onTimeout(scheduler::shutdown);
        emitter.onError((e) -> scheduler.shutdown());

        return emitter;
    }
}
