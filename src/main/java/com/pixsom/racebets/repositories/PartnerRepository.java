package com.pixsom.racebets.repositories;

import com.pixsom.racebets.entities.Partner;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerRepository extends JpaRepository<Partner, Long> {
    List<Partner> findAllByDisplayOnWaitingTrue(Sort sort);
}
