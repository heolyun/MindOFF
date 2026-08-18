package com.mindoff.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "receipt_lines")
public class ReceiptLine {
    @Id
    private UUID id;

    @Column(name = "receipt_id", nullable = false)
    private UUID receiptId;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private ReceiptItemTarget targetType;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    protected ReceiptLine() {
    }

    public ReceiptLine(
            UUID receiptId,
            String name,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            ReceiptItemTarget targetType,
            LocalDate expiresAt
    ) {
        this.id = UUID.randomUUID();
        this.receiptId = receiptId;
        this.name = name.trim();
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineTotal = lineTotal;
        this.targetType = targetType;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public UUID getReceiptId() { return receiptId; }
    public String getName() { return name; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public ReceiptItemTarget getTargetType() { return targetType; }
    public LocalDate getExpiresAt() { return expiresAt; }
}
