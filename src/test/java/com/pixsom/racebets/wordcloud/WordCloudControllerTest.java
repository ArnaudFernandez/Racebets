package com.pixsom.racebets.wordcloud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WordCloudControllerTest {

    private WordCloudService service;
    private MockMvc adminMvc;

    @BeforeEach
    void setUp() {
        service = mock(WordCloudService.class);
        adminMvc = MockMvcBuilders.standaloneSetup(new WordCloudAdminController(service)).build();
    }

    @Test
    void adminQuestionPayloadIsValidated() throws Exception {
        adminMvc.perform(post("/api/admin/word-cloud/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void moderationPayloadIsValidated() throws Exception {
        adminMvc.perform(post("/api/admin/word-cloud/questions/1/responses/censor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void playerControllerUsesJwtUserIdAndReturnsNoContentWithoutLiveQuestion() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("player@example.test")
                .claim("userId", 42L)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        WordCloudPlayerController controller = new WordCloudPlayerController(service);
        when(service.findPlayerLive(42L)).thenReturn(Optional.empty());

        var response = controller.live(jwt);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(service).findPlayerLive(42L);
    }

    @Test
    void publicLiveEndpointNeedsNoAuthenticatedPrincipal() throws Exception {
        when(service.findPublicLive()).thenReturn(Optional.empty());
        MockMvc playerMvc = MockMvcBuilders.standaloneSetup(new WordCloudPlayerController(service)).build();

        playerMvc.perform(get("/api/word-cloud/public/live"))
                .andExpect(status().isNoContent());

        verify(service).findPublicLive();
    }

    @Test
    void responseEndpointNeverAcceptsAUserIdFromPayload() {
        assertThat(com.pixsom.racebets.wordcloud.dto.WordCloudResponseRequest.class.getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly("text");
    }
}
