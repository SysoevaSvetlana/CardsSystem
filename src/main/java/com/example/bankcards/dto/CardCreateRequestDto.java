package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Запрос на создание новой карты")
public class CardCreateRequestDto {
    @Schema(description = "ID пользователя, для которого создается карта", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID пользователя обязателен")
    private Long userId;
}

