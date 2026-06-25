package com.pixsom.racebets.repositories;

import com.pixsom.racebets.entities.Race;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RaceRepository extends JpaRepository<Race, Long> {
}
