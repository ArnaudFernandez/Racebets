package com.pixsom.racebets.wordcloud;

import com.pixsom.racebets.entities.AppUser;
import com.pixsom.racebets.enums.Role;
import com.pixsom.racebets.repositories.AppUserRepository;
import com.pixsom.racebets.repositories.WordCloudQuestionRepository;
import com.pixsom.racebets.repositories.WordCloudResponseRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Component
@Profile("local")
@ConditionalOnProperty(prefix = "racebets.demo-word-cloud", name = "enabled", havingValue = "true")
public class DevWordCloudDataSeeder implements ApplicationRunner {

    static final String QUESTION_TEXT = "En un mot, que vous inspire une journée aux courses ?";
    static final String DEMO_PASSWORD = "1234";

    private static final List<String> RESPONSES = List.of(
            "Passion", "passion", "PASSION", "Passion", "Passion", "passion", "Passion", "Passion", "passion", "Passion",
            "Victoire", "victoire", "Victoire", "VICTOIRE", "Victoire", "victoire", "Victoire", "Victoire",
            "Énergie", "Energie", "énergie", "Énergie", "ENERGIE", "Énergie",
            "Partage", "partage", "Partage", "PARTAGE", "Partage",
            "Adrénaline", "Adrenaline", "adrénaline", "Adrénaline",
            "Élégance", "Elegance", "élégance",
            "Suspense", "suspense", "Suspense",
            "Vitesse", "vitesse", "Vitesse",
            "Émotion", "Emotion",
            "Fierté", "Convivialité", "Spectacle", "Chevaux"
    );

    private final WordCloudQuestionRepository questionRepository;
    private final WordCloudResponseRepository responseRepository;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DevWordCloudDataSeeder(
            WordCloudQuestionRepository questionRepository,
            WordCloudResponseRepository responseRepository,
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.questionRepository = questionRepository;
        this.responseRepository = responseRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        WordCloudQuestion question = questionRepository.findFirstByText(QUESTION_TEXT)
                .orElseGet(this::createQuestion);
        String demoPasswordHash = passwordEncoder.encode(DEMO_PASSWORD);

        for (int index = 0; index < RESPONSES.size(); index++) {
            AppUser user = findOrCreateUser(index + 1, demoPasswordHash);
            if (responseRepository.findByQuestionAndUser(question, user).isPresent()) {
                continue;
            }
            WordCloudResponse response = new WordCloudResponse();
            response.setQuestion(question);
            response.setUser(user);
            response.setDisplayText(RESPONSES.get(index));
            response.setNormalizedText(WordCloudService.normalizedText(RESPONSES.get(index)));
            responseRepository.save(response);
        }
    }

    private WordCloudQuestion createQuestion() {
        Instant now = Instant.now();
        WordCloudQuestion question = new WordCloudQuestion();
        question.setText(QUESTION_TEXT);
        question.setStatus(WordCloudQuestionStatus.CLOSED);
        question.setOpenedAt(now.minusSeconds(180));
        question.setRevealedAt(now.minusSeconds(60));
        question.setClosedAt(now);
        return questionRepository.save(question);
    }

    private AppUser findOrCreateUser(int number, String demoPasswordHash) {
        String email = "wordcloud-demo-%02d@racebets.local".formatted(number);
        AppUser user = userRepository.findByEmail(email).orElseGet(() -> {
            AppUser created = new AppUser();
            created.setName("Participant");
            created.setSurname("Démo %02d".formatted(number));
            created.setEmail(email);
            created.setPasswordHash(demoPasswordHash);
            created.setPresent(true);
            created.setTutorialCompleted(true);
            created.setRoles(Set.of(Role.USER));
            return userRepository.save(created);
        });
        if (user.getPasswordHash() == null || !passwordEncoder.matches(DEMO_PASSWORD, user.getPasswordHash())) {
            user.setPasswordHash(demoPasswordHash);
            return userRepository.save(user);
        }
        return user;
    }
}
