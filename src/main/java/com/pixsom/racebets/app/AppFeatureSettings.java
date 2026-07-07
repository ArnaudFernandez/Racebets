package com.pixsom.racebets.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "app_feature_settings")
public class AppFeatureSettings {

    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private boolean bettingEnabled = true;

    @Column(nullable = false)
    private boolean quizEnabled = true;
}
