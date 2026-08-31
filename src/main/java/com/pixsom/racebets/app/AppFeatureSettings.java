package com.pixsom.racebets.app;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AppMode activeMode = AppMode.BETTING;
}
