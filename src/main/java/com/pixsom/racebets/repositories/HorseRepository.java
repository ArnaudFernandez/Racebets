package com.pixsom.racebets.repositories;

import com.pixsom.racebets.entities.Horse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorseRepository extends JpaRepository<Horse, Long> {
}
