package com.pixsom.racebets.wordcloud;

import com.pixsom.racebets.wordcloud.dto.WordCloudAdminSnapshotResponse;
import com.pixsom.racebets.wordcloud.dto.WordCloudModerationRequest;
import com.pixsom.racebets.wordcloud.dto.WordCloudQuestionListResponse;
import com.pixsom.racebets.wordcloud.dto.WordCloudQuestionRequest;
import com.pixsom.racebets.wordcloud.dto.WordCloudSnapshotResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/word-cloud")
public class WordCloudAdminController {

    private final WordCloudService service;

    public WordCloudAdminController(WordCloudService service) {
        this.service = service;
    }

    @GetMapping("/questions")
    public List<WordCloudQuestionListResponse> questions() {
        return service.findQuestions();
    }

    @PostMapping("/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public WordCloudQuestionListResponse create(@Valid @RequestBody WordCloudQuestionRequest request) {
        return service.createQuestion(request);
    }

    @PutMapping("/questions/{id}")
    public WordCloudQuestionListResponse update(
            @PathVariable Long id,
            @Valid @RequestBody WordCloudQuestionRequest request
    ) {
        return service.updateQuestion(id, request);
    }

    @DeleteMapping("/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.deleteQuestion(id);
    }

    @GetMapping("/questions/{id}")
    public WordCloudAdminSnapshotResponse question(@PathVariable Long id) {
        return service.findAdminQuestion(id);
    }

    @PostMapping("/questions/{id}/open")
    public WordCloudSnapshotResponse open(@PathVariable Long id) {
        return service.openQuestion(id);
    }

    @PostMapping("/questions/{id}/reveal")
    public WordCloudSnapshotResponse reveal(@PathVariable Long id) {
        return service.revealQuestion(id);
    }

    @PostMapping("/questions/{id}/close")
    public WordCloudSnapshotResponse close(@PathVariable Long id) {
        return service.closeQuestion(id);
    }

    @PostMapping("/questions/{id}/reset")
    public WordCloudAdminSnapshotResponse reset(@PathVariable Long id) {
        return service.resetQuestion(id);
    }

    @PostMapping("/questions/{id}/responses/censor")
    public WordCloudAdminSnapshotResponse censor(
            @PathVariable Long id,
            @Valid @RequestBody WordCloudModerationRequest request
    ) {
        return service.censorResponse(id, request);
    }

    @GetMapping("/live")
    public ResponseEntity<WordCloudSnapshotResponse> live() {
        return service.findAdminLive()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
