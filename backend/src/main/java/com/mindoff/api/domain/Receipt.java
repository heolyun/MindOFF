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
@Table(name = "receipts")
public class Receipt {
    @Id
    private UUID id;

    @Column(name = "household_id", nullable = false)
    private UUID householdId;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "merchant_name", nullable = false, length = 160)
    private String merchantName;

    @Column(name = "purchased_at", nullable = false)
    private LocalDate purchasedAt;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "image_name", length = 300)
    private String imageName;

    @Column(name = "image_content_type", length = 120)
    private String imageContentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReceiptStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected Receipt() {
    }

    public Receipt(
            UUID householdId,
            UUID uploadedBy,
            String merchantName,
            LocalDate purchasedAt,
            BigDecimal totalAmount,
            String imageName,
            String imageContentType
    ) {
        this.id = UUID.randomUUID();
        this.householdId = householdId;
        this.uploadedBy = uploadedBy;
        this.merchantName = merchantName.trim();
        this.purchasedAt = purchasedAt;
        this.totalAmount = totalAmount;
        this.imageName = normalizeNullable(imageName);
        this.imageContentType = normalizeNullable(imageContentType);
        this.status = ReceiptStatus.DRAFT;
        this.createdAt = Instant.now();
    }

    public void confirm(String merchantName, LocalDate purchasedAt, BigDecimal totalAmount) {
        this.merchantName = merchantName.trim();
        this.purchasedAt = purchasedAt;
        this.totalAmount = totalAmount;
        this.status = ReceiptStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public UUID getId() { return id; }
    public UUID getHouseholdId() { return householdId; }
    public UUID getUploadedBy() { return uploadedBy; }
    public String getMerchantName() { return merchantName; }
    public LocalDate getPurchasedAt() { return purchasedAt; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getImageName() { return imageName; }
    public String getImageContentType() { return imageContentType; }
    public ReceiptStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
}
