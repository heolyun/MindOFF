package com.mindoff.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "fridge_items")
public class FridgeItem {
    @Id
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "purchased_at", nullable = false)
    private LocalDate purchasedAt;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FridgeStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FridgeItem() {
    }

    public FridgeItem(UUID householdId, String name, LocalDate purchasedAt, LocalDate expiresAt) {
        this.id = UUID.randomUUID();
        this.householdId = householdId;
        this.name = name.trim();
        this.purchasedAt = purchasedAt;
        this.expiresAt = expiresAt;
        this.status = FridgeStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public void finish() {
        this.status = FridgeStatus.FINISHED;
    }

    public UUID getId() { return id; }
    public UUID getHouseholdId() { return householdId; }
    public String getName() { return name; }
    public LocalDate getPurchasedAt() { return purchasedAt; }
    public LocalDate getExpiresAt() { return expiresAt; }
    public FridgeStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
