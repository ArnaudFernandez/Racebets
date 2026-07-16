package com.pixsom.racebets.entities;

import com.pixsom.racebets.entities.common.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "partners")
public class Partner extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Lob
    @Column(name = "logo_data", nullable = false)
    private byte[] logoData;

    @Column(name = "logo_content_type", nullable = false, length = 40)
    private String logoContentType;

    @Column(name = "display_on_waiting", nullable = false)
    private boolean displayOnWaiting;
}
