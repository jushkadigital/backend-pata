package com.microservice.quarkus.user.admin.infrastructure.db.hibernate.dbo;

import java.time.Instant;

import com.microservice.quarkus.user.admin.infrastructure.db.hibernate.dbo.converter.EmailAddressConverter;
import com.microservice.quarkus.user.shared.domain.EmailAddress;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "admins", schema = "quarkus")
@Getter
@Setter
public class AdminEntity {
    @Id
    private String id;

    @Convert(converter = EmailAddressConverter.class)
    @Column(name = "email", nullable = false)
    private EmailAddress email;

    @Column(name = "admin_type", nullable = false)
    private String type;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
