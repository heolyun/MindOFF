package com.mindoff.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "household_members")
public class HouseholdMember {
    @Id
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HouseholdRole role;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected HouseholdMember() {
    }

    public HouseholdMember(UUID householdId, UUID userId, HouseholdRole role) {
        this.id = UUID.randomUUID();
        this.householdId = householdId;
        this.userId = userId;
        this.role = role;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getHouseholdId() { return householdId; }
    public UUID getUserId() { return userId; }
    public HouseholdRole getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}
