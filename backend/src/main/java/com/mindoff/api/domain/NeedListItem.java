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
@Table(name = "need_list_items")
public class NeedListItem {
    @Id
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private NeedSourceType sourceType;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "purchase_url", length = 1000)
    private String purchaseUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NeedListStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected NeedListItem() {
    }

    public NeedListItem(
            UUID householdId,
            NeedSourceType sourceType,
            UUID sourceId,
            String name,
            String purchaseUrl
    ) {
        this.id = UUID.randomUUID();
        this.householdId = householdId;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.name = name.trim();
        this.purchaseUrl = purchaseUrl == null || purchaseUrl.isBlank() ? null : purchaseUrl.trim();
        this.status = NeedListStatus.NEEDED;
        this.createdAt = Instant.now();
    }

    public void complete() {
        this.status = NeedListStatus.PURCHASED;
        this.completedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getHouseholdId() { return householdId; }
    public NeedSourceType getSourceType() { return sourceType; }
    public UUID getSourceId() { return sourceId; }
    public String getName() { return name; }
    public String getPurchaseUrl() { return purchaseUrl; }
    public NeedListStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
