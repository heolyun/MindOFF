package com.mindoff.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class Subscription {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    private BillingCycle billingCycle;

    @Column(name = "next_billing_at")
    private LocalDate nextBillingAt;

    @Column(name = "trial_end_at")
    private LocalDate trialEndAt;

    @Column(name = "management_url", length = 1000)
    private String managementUrl;

    @Column(nullable = false)
    private boolean shared;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Subscription() {
    }

    public Subscription(
            UUID userId,
            String name,
            BigDecimal amount,
            BillingCycle billingCycle,
            LocalDate nextBillingAt,
            LocalDate trialEndAt,
            String managementUrl,
            boolean shared
    ) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.name = name.trim();
        this.amount = amount;
        this.billingCycle = billingCycle;
        this.nextBillingAt = nextBillingAt;
        this.trialEndAt = trialEndAt;
        this.managementUrl = managementUrl == null || managementUrl.isBlank() ? null : managementUrl.trim();
        this.shared = shared;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public BigDecimal getAmount() { return amount; }
    public BillingCycle getBillingCycle() { return billingCycle; }
    public LocalDate getNextBillingAt() { return nextBillingAt; }
    public LocalDate getTrialEndAt() { return trialEndAt; }
    public String getManagementUrl() { return managementUrl; }
    public boolean isShared() { return shared; }
    public Instant getCreatedAt() { return createdAt; }
}
