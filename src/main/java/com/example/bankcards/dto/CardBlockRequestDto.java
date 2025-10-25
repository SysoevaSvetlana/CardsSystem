package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Запрос на блокировку карты")
public class CardBlockRequestDto {
    @Schema(description = "ID карты для блокировки", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID карты обязателен")
    private Long cardId;
}

