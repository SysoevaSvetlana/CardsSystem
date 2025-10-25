package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Информация о банковской карте")
public class CardResponseDto {
    @Schema(description = "ID карты", example = "1")
    private Long id;

    @Schema(description = "Маскированный номер карты", example = "**** **** **** 1234")
    private String maskedNumber;

    @Schema(description = "Баланс карты", example = "1500.50")
    private BigDecimal balance;

    @Schema(description = "Статус карты", example = "ACTIVE", allowableValues = {"ACTIVE", "BLOCKED", "BLOCK_REQUESTED", "EXPIRED"})
    private String status;

    @Schema(description = "Дата истечения срока действия", example = "2026-12-31")
    private LocalDate expiryDate;

    @Schema(description = "ID владельца карты", example = "1")
    private Long userId;
}
