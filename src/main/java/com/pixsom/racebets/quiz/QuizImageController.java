package com.pixsom.racebets.quiz;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/quizzes/sessions/{sessionId}/questions/{questionId}/images")
public class QuizImageController {

    private final QuizImageService imageService;

    public QuizImageController(QuizImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping("/{kind:question|answer}")
    public ResponseEntity<byte[]> image(@PathVariable Long sessionId,
                                        @PathVariable Long questionId,
                                        @PathVariable String kind,
                                        @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        QuizImageService.ImageKind imageKind = "question".equals(kind)
                ? QuizImageService.ImageKind.QUESTION
                : QuizImageService.ImageKind.ANSWER;
        QuizImageService.ImageResponse image = imageService.findSessionImage(sessionId, questionId, imageKind);

        ResponseEntity.BodyBuilder response = ResponseEntity.status(
                        image.etag().equals(ifNoneMatch) ? HttpStatus.NOT_MODIFIED : HttpStatus.OK)
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noCache().cachePrivate())
                .eTag(image.etag())
                .varyBy(HttpHeaders.AUTHORIZATION)
                .header("X-Content-Type-Options", "nosniff");
        return image.etag().equals(ifNoneMatch) ? response.build() : response.body(image.bytes());
    }
}
