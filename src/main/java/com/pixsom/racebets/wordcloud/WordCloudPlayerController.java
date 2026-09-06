package com.pixsom.racebets.wordcloud;

import com.pixsom.racebets.wordcloud.dto.WordCloudResponseRequest;
import com.pixsom.racebets.wordcloud.dto.WordCloudSnapshotResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/word-cloud")
public class WordCloudPlayerController {

    private final WordCloudService service;

    public WordCloudPlayerController(WordCloudService service) {
        this.service = service;
    }

    @GetMapping("/live")
    public ResponseEntity<WordCloudSnapshotResponse> live(@AuthenticationPrincipal Jwt jwt) {
        return service.findPlayerLive(userId(jwt))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/public/live")
    public ResponseEntity<WordCloudSnapshotResponse> publicLive() {
        return service.findPublicLive()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/questions/{id}/responses")
    public WordCloudSnapshotResponse respond(
            @PathVariable Long id,
            @Valid @RequestBody WordCloudResponseRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.submitResponse(id, request.text(), userId(jwt));
    }

    private Long userId(Jwt jwt) {
        return jwt.getClaim("userId");
    }
}
