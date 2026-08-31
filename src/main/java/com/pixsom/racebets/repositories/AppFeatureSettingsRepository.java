package com.pixsom.racebets.repositories;

import com.pixsom.racebets.app.AppFeatureSettings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppFeatureSettingsRepository extends JpaRepository<AppFeatureSettings, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select settings from AppFeatureSettings settings where settings.id = :id")
    Optional<AppFeatureSettings> findLockedById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select settings from AppFeatureSettings settings where settings.id = :id")
    Optional<AppFeatureSettings> findReadLockedById(@Param("id") Long id);
}
