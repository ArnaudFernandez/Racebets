package com.pixsom.racebets.wordcloud;

import com.pixsom.racebets.repositories.WordCloudQuestionRepository;
import com.pixsom.racebets.repositories.WordCloudResponseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("local")
@SpringBootTest
class DevWordCloudDataSeederTest {

    @Autowired
    private WordCloudQuestionRepository questionRepository;

    @Autowired
    private WordCloudResponseRepository responseRepository;

    @Autowired
    private com.pixsom.racebets.repositories.AppUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void localDatasetContainsAtLeastFortyResponsesWithRepeatedValues() {
        WordCloudQuestion question = questionRepository.findFirstByText(DevWordCloudDataSeeder.QUESTION_TEXT)
                .orElseThrow();
        var responses = responseRepository.findByQuestionOrderByCreatedAtAscIdAsc(question);

        assertThat(question.getStatus()).isEqualTo(WordCloudQuestionStatus.CLOSED);
        assertThat(responses).hasSizeGreaterThanOrEqualTo(40);
        assertThat(responses.stream().map(WordCloudResponse::getNormalizedText).distinct().count())
                .isLessThan(responses.size());
        assertThat(passwordEncoder.matches(
                DevWordCloudDataSeeder.DEMO_PASSWORD,
                userRepository.findByEmail("wordcloud-demo-01@racebets.local").orElseThrow().getPasswordHash()
        )).isTrue();
    }
}
