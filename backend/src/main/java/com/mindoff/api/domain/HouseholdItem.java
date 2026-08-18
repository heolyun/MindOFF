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
@Table(name = "household_items")
public class HouseholdItem {
    @Id
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "purchased_at", nullable = false)
    private LocalDate purchasedAt;

    @Column(name = "finished_at")
    private LocalDate finishedAt;

    @Column(name = "predicted_days")
    private Integer predictedDays;

    @Column(name = "repeat_purchase", nullable = false)
    private boolean repeatPurchase;

    @Column(name = "purchase_url", length = 1000)
    private String purchaseUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HouseholdItemStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected HouseholdItem() {
    }

    public HouseholdItem(
            UUID householdId,
            String name,
            LocalDate purchasedAt,
            boolean repeatPurchase,
            String purchaseUrl
    ) {
        this.id = UUID.randomUUID();
        this.householdId = householdId;
        this.name = name.trim();
        this.purchasedAt = purchasedAt;
        this.repeatPurchase = repeatPurchase;
        this.purchaseUrl = normalizeNullable(purchaseUrl);
        this.status = HouseholdItemStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public void finish(LocalDate finishedAt, int predictedDays) {
        this.finishedAt = finishedAt;
        this.predictedDays = predictedDays;
        this.status = HouseholdItemStatus.FINISHED;
    }

    public void applyPrediction(Integer predictedDays) {
        this.predictedDays = predictedDays;
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getHouseholdId() { return householdId; }
    public String getName() { return name; }
    public LocalDate getPurchasedAt() { return purchasedAt; }
    public LocalDate getFinishedAt() { return finishedAt; }
    public Integer getPredictedDays() { return predictedDays; }
    public boolean isRepeatPurchase() { return repeatPurchase; }
    public String getPurchaseUrl() { return purchaseUrl; }
    public HouseholdItemStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
