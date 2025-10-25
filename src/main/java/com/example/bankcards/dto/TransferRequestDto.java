package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Запрос на перевод между картами")
public class TransferRequestDto {
    @Schema(description = "ID карты отправителя", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID карты отправителя обязателен")
    private Long fromCardId;

    @Schema(description = "ID карты получателя", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID карты получателя обязателен")
    private Long toCardId;

    @Schema(description = "Сумма перевода", example = "100.50", requiredMode = Schema.RequiredMode.REQUIRED)
    @DecimalMin(value = "0.01", message = "Минимальная сумма перевода — 0.01")
    private BigDecimal amount;
}
