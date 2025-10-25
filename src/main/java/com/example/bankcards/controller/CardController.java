package com.example.bankcards.controller;

import com.example.bankcards.dto.CardBlockRequestDto;
import com.example.bankcards.dto.CardCreateRequestDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.entity.User;
import com.example.bankcards.service.CardService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Карты", description = "Управление банковскими картами")
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardService cardService;
    private final UserService userService;

    // ----------------- ADMIN -----------------

    @Operation(
        summary = "Создать карту (ADMIN)",
        description = "Создает новую карту для указанного пользователя. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Карта успешно создана"),
        @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CardResponseDto> createCard(@Valid @RequestBody CardCreateRequestDto dto) {
        CardResponseDto response = cardService.createCard(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "Активировать карту (ADMIN)",
        description = "Активирует заблокированную карту. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Карта активирована"),
        @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{cardId}/activate")
    public ResponseEntity<CardResponseDto> activateCard(
        @Parameter(description = "ID карты", required = true) @PathVariable(name = "cardId") Long cardId
    ) {
        return ResponseEntity.ok(cardService.activateCard(cardId));
    }

    @Operation(
        summary = "Подтвердить блокировку карты (ADMIN)",
        description = "Подтверждает запрос пользователя на блокировку карты. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Блокировка подтверждена"),
        @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{cardId}/confirm-block")
    public ResponseEntity<CardResponseDto> confirmBlock(
        @Parameter(description = "ID карты", required = true) @PathVariable(name = "cardId") Long cardId
    ) {
        return ResponseEntity.ok(cardService.confirmBlock(cardId));
    }

    @Operation(
        summary = "Отклонить блокировку карты (ADMIN)",
        description = "Отклоняет запрос пользователя на блокировку карты. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Блокировка отклонена"),
        @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{cardId}/reject-block")
    public ResponseEntity<CardResponseDto> rejectBlock(
        @Parameter(description = "ID карты", required = true) @PathVariable(name = "cardId") Long cardId
    ) {
        return ResponseEntity.ok(cardService.rejectBlock(cardId));
    }

    @Operation(
        summary = "Удалить карту (ADMIN)",
        description = "Удаляет карту из системы. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Карта успешно удалена"),
        @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{cardId}")
    public ResponseEntity<Void> deleteCard(
        @Parameter(description = "ID карты", required = true) @PathVariable(name = "cardId") Long cardId
    ) {
        cardService.deleteCard(cardId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Получить все карты (ADMIN)",
        description = "Получает список всех карт в системе с пагинацией. Доступно только администраторам."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список карт"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<Page<CardResponseDto>> getAllCards(
            @Parameter(description = "Номер страницы (начиная с 0)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Количество элементов на странице") @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(cardService.findAllCards(pageable));
    }

    // ----------------- USER -----------------

    @Operation(
        summary = "Получить свои карты (USER)",
        description = "Получает список карт текущего пользователя с пагинацией"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Список карт пользователя"),
        @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/my")
    public ResponseEntity<Page<CardResponseDto>> getMyCards(
            @Parameter(description = "Номер страницы (начиная с 0)") @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Количество элементов на странице") @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        User currentUser = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(cardService.findUserCards(currentUser.getId(), pageable));
    }

    @Operation(
        summary = "Запросить блокировку карты (USER)",
        description = "Пользователь запрашивает блокировку своей карты. Требуется подтверждение администратора."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Запрос на блокировку отправлен"),
        @ApiResponse(responseCode = "400", description = "Ошибка (карта уже заблокирована или не принадлежит пользователю)")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @PatchMapping("/request-block")
    public ResponseEntity<CardResponseDto> requestBlock(@Valid @RequestBody CardBlockRequestDto dto) {
        return ResponseEntity.ok(cardService.requestBlock(dto));
    }

    @Operation(
        summary = "Получить баланс карты (USER)",
        description = "Получает баланс указанной карты пользователя"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Баланс карты"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещен (чужая карта)"),
        @ApiResponse(responseCode = "404", description = "Карта не найдена")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @GetMapping("/{cardId}/balance")
    public ResponseEntity<BigDecimal> getBalance(
        @Parameter(description = "ID карты", required = true) @PathVariable(name = "cardId") Long cardId
    ) {
        return ResponseEntity.ok(cardService.getCardBalance(cardId));
    }
}
