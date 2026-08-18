package com.mindoff.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "household_invitations")
public class HouseholdInvitation {
    @Id
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(name = "invited_by", nullable = false)
    private UUID invitedBy;

    @Column(nullable = false, unique = true, length = 100)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HouseholdInvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    protected HouseholdInvitation() {
    }

    public HouseholdInvitation(UUID householdId, String email, UUID invitedBy, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.householdId = householdId;
        this.email = email.trim().toLowerCase(Locale.ROOT);
        this.invitedBy = invitedBy;
        this.token = UUID.randomUUID().toString();
        this.status = HouseholdInvitationStatus.PENDING;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public void expire() {
        this.status = HouseholdInvitationStatus.EXPIRED;
    }

    public void accept(Instant now) {
        this.status = HouseholdInvitationStatus.ACCEPTED;
        this.acceptedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getHouseholdId() { return householdId; }
    public String getEmail() { return email; }
    public UUID getInvitedBy() { return invitedBy; }
    public String getToken() { return token; }
    public HouseholdInvitationStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
}
