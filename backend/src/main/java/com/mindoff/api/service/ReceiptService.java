package com.mindoff.api.service;

import com.mindoff.api.domain.Receipt;
import com.mindoff.api.domain.ReceiptItemTarget;
import com.mindoff.api.domain.ReceiptLine;
import com.mindoff.api.domain.ReceiptStatus;
import com.mindoff.api.repository.ReceiptLineRepository;
import com.mindoff.api.repository.ReceiptRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReceiptService {
    private final AccessService accessService;
    private final InventoryService inventoryService;
    private final ReceiptOcrGateway ocrGateway;
    private final ReceiptRepository receiptRepository;
    private final ReceiptLineRepository lineRepository;

    public ReceiptService(
            AccessService accessService,
            InventoryService inventoryService,
            ReceiptOcrGateway ocrGateway,
            ReceiptRepository receiptRepository,
            ReceiptLineRepository lineRepository
    ) {
        this.accessService = accessService;
        this.inventoryService = inventoryService;
        this.ocrGateway = ocrGateway;
        this.receiptRepository = receiptRepository;
        this.lineRepository = lineRepository;
    }

    @Transactional
    public ReceiptView intake(
            UUID householdId,
            UUID userId,
            String fileName,
            String contentType,
            byte[] content
    ) {
        accessService.requireMember(householdId, userId);
        ReceiptOcrGateway.OcrDraft draft = ocrGateway.analyze(fileName, contentType, content);
        Receipt receipt = receiptRepository.save(new Receipt(
                householdId,
                userId,
                draft.merchantName(),
                draft.purchasedAt(),
                draft.totalAmount(),
                fileName,
                contentType
        ));
        List<ReceiptLine> lines = draft.lines().stream()
                .map(line -> toEntity(receipt.getId(), line))
                .map(lineRepository::save)
                .toList();
        return new ReceiptView(receipt, lines);
    }

    @Transactional(readOnly = true)
    public List<ReceiptView> list(UUID householdId, UUID userId) {
        accessService.requireMember(householdId, userId);
        return receiptRepository.findAllByHouseholdIdOrderByCreatedAtDesc(householdId).stream()
                .map(receipt -> new ReceiptView(
                        receipt,
                        lineRepository.findAllByReceiptIdOrderById(receipt.getId())
                ))
                .toList();
    }

    @Transactional
    public ReceiptView confirm(UUID receiptId, UUID userId, ReceiptReview review) {
        Receipt receipt = receiptRepository.findById(receiptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "영수증을 찾을 수 없습니다."));
        accessService.requireMember(receipt.getHouseholdId(), userId);
        if (receipt.getStatus() == ReceiptStatus.CONFIRMED) {
            return new ReceiptView(receipt, lineRepository.findAllByReceiptIdOrderById(receiptId));
        }

        lineRepository.deleteAllByReceiptId(receiptId);
        List<ReceiptLine> lines = review.lines().stream()
                .map(line -> new ReceiptLine(
                        receiptId,
                        line.name(),
                        line.quantity(),
                        line.unitPrice(),
                        line.lineTotal(),
                        line.targetType(),
                        line.expiresAt()
                ))
                .map(lineRepository::save)
                .toList();

        for (ReceiptLine line : lines) {
            if (line.getTargetType() == ReceiptItemTarget.FRIDGE) {
                inventoryService.addFridgeItem(
                        receipt.getHouseholdId(),
                        userId,
                        line.getName(),
                        review.purchasedAt(),
                        line.getExpiresAt()
                );
            } else if (line.getTargetType() == ReceiptItemTarget.HOUSEHOLD_ITEM) {
                inventoryService.addHouseholdItem(
                        receipt.getHouseholdId(),
                        userId,
                        line.getName(),
                        review.purchasedAt(),
                        true,
                        null
                );
            }
        }

        receipt.confirm(review.merchantName(), review.purchasedAt(), review.totalAmount());
        return new ReceiptView(receipt, lines);
    }

    private static ReceiptLine toEntity(UUID receiptId, ReceiptOcrGateway.OcrLine line) {
        return new ReceiptLine(
                receiptId,
                line.name(),
                line.quantity(),
                line.unitPrice(),
                line.lineTotal(),
                line.targetType(),
                line.expiresAt()
        );
    }

    public record ReceiptView(Receipt receipt, List<ReceiptLine> lines) {
    }

    public record ReceiptReview(
            String merchantName,
            LocalDate purchasedAt,
            BigDecimal totalAmount,
            List<ReviewedLine> lines
    ) {
    }

    public record ReviewedLine(
            String name,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            ReceiptItemTarget targetType,
            LocalDate expiresAt
    ) {
    }
}
