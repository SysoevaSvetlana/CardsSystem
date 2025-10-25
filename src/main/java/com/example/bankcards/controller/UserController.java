package com.example.bankcards.controller;

import com.example.bankcards.dto.UserResponseDto;
import com.example.bankcards.dto.UserRoleUpdateDto;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Пользователи (ADMIN)", description = "Управление пользователями. Доступно только администраторам.")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(
        summary = "Получить всех пользователей (ADMIN)",
        description = "Получает список всех пользователей с пагинацией. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Список пользователей",
            content = @Content(schema = @Schema(implementation = Page.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Недостаточно прав"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<UserResponseDto>> getAllUsers(
            @Parameter(description = "Номер страницы (начиная с 0)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Количество элементов на странице") @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<User> users = userService.findAll(pageable);
        Page<UserResponseDto> response = users.map(this::mapToResponse);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Назначить роль пользователю (ADMIN)",
        description = "Изменяет роль пользователя (USER или ADMIN). Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Роль успешно изменена",
            content = @Content(schema = @Schema(implementation = UserResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Пользователь не найден"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponseDto> updateUserRole(
            @Parameter(description = "ID пользователя", required = true) @PathVariable(name = "id") Long id,
            @Valid @RequestBody UserRoleUpdateDto dto
    ) {
        User updated = userService.assignRole(id, Role.valueOf(dto.getRole()));
        return ResponseEntity.ok(mapToResponse(updated));
    }

    @Operation(
        summary = "Удалить пользователя (ADMIN)",
        description = "Удаляет пользователя из системы. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Пользователь успешно удален"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Пользователь не найден"
        )
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
        @Parameter(description = "ID пользователя", required = true) @PathVariable(name = "id") Long id
    ) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }


    private UserResponseDto mapToResponse(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        return dto;
    }


}
