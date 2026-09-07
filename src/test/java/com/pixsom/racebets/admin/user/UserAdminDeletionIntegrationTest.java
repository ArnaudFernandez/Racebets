package com.pixsom.racebets.admin.user;

import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;
import com.pixsom.racebets.repositories.AppUserRepository;
import com.pixsom.racebets.repositories.WordCloudQuestionRepository;
import com.pixsom.racebets.repositories.WordCloudResponseRepository;
import com.pixsom.racebets.wordcloud.WordCloudQuestion;
import com.pixsom.racebets.wordcloud.WordCloudResponse;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserAdminDeletionIntegrationTest {

    @Autowired UserAdminService userAdminService;
    @Autowired AppUserRepository userRepository;
    @Autowired WordCloudQuestionRepository questionRepository;
    @Autowired WordCloudResponseRepository responseRepository;
    @Autowired EntityManager entityManager;

    @Test
    void deletesUserAndOwnedWordCloudResponses() {
        AppUser user = new AppUser();
        user.setName("Louise");
        user.setSurname("Michel");
        user.setEmail("louise.michel@example.test");
        user.setPresent(true);
        user.setRoles(Set.of(Role.USER));
        user = userRepository.save(user);

        WordCloudQuestion question = new WordCloudQuestion();
        question.setText("Votre destination favorite ?");
        question = questionRepository.save(question);

        WordCloudResponse response = new WordCloudResponse();
        response.setQuestion(question);
        response.setUser(user);
        response.setDisplayText("Paris");
        response.setNormalizedText("paris");
        responseRepository.saveAndFlush(response);
        entityManager.clear();

        userAdminService.delete(user.getId());

        assertThat(userRepository.existsById(user.getId())).isFalse();
        assertThat(responseRepository.count()).isZero();
        assertThat(questionRepository.existsById(question.getId())).isTrue();
    }
}
