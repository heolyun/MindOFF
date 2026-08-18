package com.mindoff.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "usage_cycles")
public class UsageCycle {
    @Id
    private UUID id;

    @Column(name = "household_item_id", nullable = false)
    private UUID householdItemId;

    @Column(name = "started_at", nullable = false)
    private LocalDate startedAt;

    @Column(name = "finished_at", nullable = false)
    private LocalDate finishedAt;

    @Column(name = "duration_days", nullable = false)
    private int durationDays;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UsageCycle() {
    }

    public UsageCycle(UUID householdItemId, LocalDate startedAt, LocalDate finishedAt, int durationDays) {
        this.id = UUID.randomUUID();
        this.householdItemId = householdItemId;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.durationDays = durationDays;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getHouseholdItemId() { return householdItemId; }
    public LocalDate getStartedAt() { return startedAt; }
    public LocalDate getFinishedAt() { return finishedAt; }
    public int getDurationDays() { return durationDays; }
    public Instant getCreatedAt() { return createdAt; }
}
