package com.example.bankcards.controller;

import com.example.bankcards.dto.TransferRequestDto;
import com.example.bankcards.dto.TransferResponseDto;
import com.example.bankcards.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
@Tag(name = "Переводы", description = "Переводы между картами")
@SecurityRequirement(name = "bearerAuth")
public class TransferController {

    private final TransferService transferService;

    @Operation(
        summary = "Перевод между картами",
        description = "Выполняет перевод средств между картами пользователя. Обе карты должны принадлежать текущему пользователю и быть активными."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Перевод выполнен успешно",
            content = @Content(schema = @Schema(implementation = TransferResponseDto.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Ошибка валидации или недостаточно средств"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Карта не найдена"
        )
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PostMapping
    public ResponseEntity<TransferResponseDto> transfer(@Valid @RequestBody TransferRequestDto dto) {
        TransferResponseDto response = transferService.transferBetweenCards(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}