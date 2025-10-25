package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "Информация о переводе")
public class TransferResponseDto {
    @Schema(description = "ID перевода", example = "1")
    private Long id;

    @Schema(description = "Маскированный номер карты отправителя", example = "**** **** **** 1234")
    private String fromCardMaskedNumber;

    @Schema(description = "Маскированный номер карты получателя", example = "**** **** **** 5678")
    private String toCardMaskedNumber;

    @Schema(description = "Сумма перевода", example = "100.50")
    private BigDecimal amount;

    @Schema(description = "Статус перевода", example = "SUCCESS")
    private String status;

    @Schema(description = "Дата и время создания перевода", example = "2025-10-25T14:30:00")
    private LocalDateTime createdAt;
}