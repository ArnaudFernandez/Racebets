package com.pixsom.racebets.repositories;

import com.pixsom.racebets.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AppUser> findLockedByEmail(String email);

    boolean existsByEmail(String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AppUser> findLockedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from AppUser user")
    List<AppUser> findAllForUpdate();
}
