package com.mindoff.api.web;

import com.mindoff.api.domain.ReceiptItemTarget;
import com.mindoff.api.service.ReceiptService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ReceiptController {
    private final ReceiptService receiptService;

    public ReceiptController(ReceiptService receiptService) {
        this.receiptService = receiptService;
    }

    @GetMapping("/households/{householdId}/receipts")
    public List<ReceiptService.ReceiptView> list(
            @PathVariable UUID householdId,
            @RequestParam UUID userId
    ) {
        return receiptService.list(householdId, userId);
    }

    @PostMapping(
            value = "/households/{householdId}/receipts/intake",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ReceiptService.ReceiptView intake(
            @PathVariable UUID householdId,
            @RequestPart UUID userId,
            @RequestPart MultipartFile file
    ) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("영수증 이미지가 비어 있습니다.");
        }
        return receiptService.intake(
                householdId,
                userId,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getBytes()
        );
    }

    @PatchMapping("/receipts/{receiptId}/confirm")
    public ReceiptService.ReceiptView confirm(
            @PathVariable UUID receiptId,
            @Valid @RequestBody ConfirmReceiptRequest request
    ) {
        return receiptService.confirm(receiptId, request.userId(), new ReceiptService.ReceiptReview(
                request.merchantName(),
                request.purchasedAt(),
                request.totalAmount(),
                request.lines().stream().map(line -> new ReceiptService.ReviewedLine(
                        line.name(),
                        line.quantity(),
                        line.unitPrice(),
                        line.lineTotal(),
                        line.targetType(),
                        line.expiresAt()
                )).toList()
        ));
    }

    public record ConfirmReceiptRequest(
            @NotNull UUID userId,
            @NotBlank @Size(max = 160) String merchantName,
            @NotNull LocalDate purchasedAt,
            @NotNull @DecimalMin("0") BigDecimal totalAmount,
            @NotEmpty List<@Valid ConfirmReceiptLineRequest> lines
    ) {
    }

    public record ConfirmReceiptLineRequest(
            @NotBlank @Size(max = 160) String name,
            @NotNull @DecimalMin("0.01") BigDecimal quantity,
            @NotNull @DecimalMin("0") BigDecimal unitPrice,
            @NotNull @DecimalMin("0") BigDecimal lineTotal,
            @NotNull ReceiptItemTarget targetType,
            LocalDate expiresAt
    ) {
    }
}
