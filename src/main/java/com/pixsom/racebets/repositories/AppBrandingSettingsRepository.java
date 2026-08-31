package com.pixsom.racebets.repositories;

import com.pixsom.racebets.app.branding.AppBrandingSettings;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AppBrandingSettingsRepository extends JpaRepository<AppBrandingSettings, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select settings from AppBrandingSettings settings where settings.id = :id")
    Optional<AppBrandingSettings> findLockedById(@Param("id") Long id);
}
