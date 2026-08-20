package com.mindoff.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.mindoff.api.service.ReceiptService;
import com.mindoff.api.web.ReceiptController;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ReceiptControllerValidationTests {
    private final ReceiptController controller = new ReceiptController(mock(ReceiptService.class));

    @Test
    void emptyReceiptImageIsRejected() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> controller.intake(UUID.randomUUID(), UUID.randomUUID(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("영수증 이미지가 비어 있습니다.");
    }

    @Test
    void unsupportedReceiptImageTypeIsRejected() {
        MockMultipartFile file = new MockMultipartFile("file", "receipt.gif", "image/gif", new byte[] {1});

        assertThatThrownBy(() -> controller.intake(UUID.randomUUID(), UUID.randomUUID(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("JPG 또는 PNG 영수증 이미지만 업로드할 수 있습니다.");
    }

    @Test
    void receiptImageOverTenMegabytesIsRejected() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipt.png",
                "image/png",
                new byte[10 * 1024 * 1024 + 1]
        );

        assertThatThrownBy(() -> controller.intake(UUID.randomUUID(), UUID.randomUUID(), file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("영수증 이미지는 10MB 이하여야 합니다.");
    }
}
