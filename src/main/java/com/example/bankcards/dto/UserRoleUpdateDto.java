package com.example.bankcards.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Запрос на обновление роли пользователя")
public class UserRoleUpdateDto {
    @NotBlank(message = "Роль обязательна")
    @Schema(description = "Новая роль", example = "ROLE_ADMIN")
    private String role;
}