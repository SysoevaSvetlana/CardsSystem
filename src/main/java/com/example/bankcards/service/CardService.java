package com.example.bankcards.service;

import com.example.bankcards.dto.CardBlockRequestDto;
import com.example.bankcards.dto.CardCreateRequestDto;
import com.example.bankcards.dto.CardResponseDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Сервис для управления банковскими картами.
 * Обеспечивает создание, активацию, блокировку и удаление карт.
 */
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    /**
     * Создание карты администратором.
     *
     * @param dto данные для создания карты
     * @return созданная карта
     * @throws UserNotFoundException если пользователь не найден
     */
    public CardResponseDto createCard(CardCreateRequestDto dto) {
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new UserNotFoundException(dto.getUserId()));

        Card card = new Card();
        card.setUser(user);
        card.setExpiryDate(LocalDate.now().plusYears(3));

        String generatedNumber = generateCardNumber();
        card.setCardNumber(generatedNumber);

        Card saved = cardRepository.save(card);
        return mapToResponse(saved);
    }

    /**
     * Запрос блокировки карты пользователем.
     *
     * @param dto данные запроса блокировки
     * @return обновленная карта
     * @throws CardNotFoundException если карта не найдена
     * @throws CardException если карта уже заблокирована или принадлежит другому пользователю
     */
    public CardResponseDto requestBlock(CardBlockRequestDto dto) {
        User currentUser = userService.getCurrentUser();
        Card card = cardRepository.findById(dto.getCardId())
                .orElseThrow(() -> new CardNotFoundException(dto.getCardId()));

        if (!card.getUser().getId().equals(currentUser.getId())) {
            throw new CardException("Нельзя блокировать чужую карту");
        }

        if (card.getStatus() == CardStatus.BLOCKED) {
            throw new CardException("Карта уже заблокирована");
        }

        card.setStatus(CardStatus.BLOCK_REQUESTED);
        return mapToResponse(cardRepository.save(card));
    }
    /**
     * Активация карты администратором.
     *
     * @param cardId ID карты
     * @return обновленная карта
     * @throws CardNotFoundException если карта не найдена
     * @throws CardException если карта уже активна
     */
    public CardResponseDto activateCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        if (card.getStatus() == CardStatus.ACTIVE) {
            throw new CardException("Карта уже активна");
        }

        card.setStatus(CardStatus.ACTIVE);
        return mapToResponse(cardRepository.save(card));
    }

    /**
     * Подтверждение блокировки карты администратором.
     *
     * @param cardId ID карты
     * @return обновленная карта
     * @throws CardNotFoundException если карта не найдена
     */
    public CardResponseDto confirmBlock(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));
        card.setStatus(CardStatus.BLOCKED);
        return mapToResponse(cardRepository.save(card));
    }

    /**
     * Отклонение блокировки карты администратором.
     *
     * @param cardId ID карты
     * @return обновленная карта
     * @throws CardNotFoundException если карта не найдена
     */
    public CardResponseDto rejectBlock(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));
        card.setStatus(CardStatus.ACTIVE);
        return mapToResponse(cardRepository.save(card));
    }

    /**
     * Удаление карты.
     *
     * @param cardId ID карты
     */
    public void deleteCard(Long cardId) {
        cardRepository.deleteById(cardId);
    }

    /**
     * Получение всех карт пользователя (постранично).
     *
     * @param userId ID пользователя
     * @param pageable параметры пагинации
     * @return страница карт пользователя
     */
    public Page<CardResponseDto> findUserCards(Long userId, Pageable pageable) {
        return cardRepository.findByUserId(userId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Получение всех карт (для администратора).
     *
     * @param pageable параметры пагинации
     * @return страница всех карт
     */
    public Page<CardResponseDto> findAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Получение баланса карты текущего пользователя.
     *
     * @param cardId ID карты
     * @return баланс карты
     * @throws CardNotFoundException если карта не найдена
     * @throws CardException если карта принадлежит другому пользователю
     */
    public BigDecimal getCardBalance(Long cardId) {
        User currentUser = userService.getCurrentUser();
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        if (!card.getUser().getId().equals(currentUser.getId())) {
            throw new CardException("Доступ запрещён — чужая карта");
        }

        return card.getBalance();
    }

    /**
     * Получение всех карт текущего пользователя.
     *
     * @return список карт пользователя
     */
    public List<CardResponseDto> getUserCards() {
        User currentUser = userService.getCurrentUser();
        return cardRepository.findByUserId(currentUser.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Преобразование сущности Card в DTO.
     *
     * @param card сущность карты
     * @return DTO карты
     */
    private CardResponseDto mapToResponse(Card card) {
        CardResponseDto dto = new CardResponseDto();
        dto.setId(card.getId());
        dto.setMaskedNumber(card.getMaskedNumber());
        dto.setBalance(card.getBalance());
        dto.setStatus(card.getStatus().name());
        dto.setExpiryDate(card.getExpiryDate());
        dto.setUserId(card.getUser().getId());
        return dto;
    }

    /**
     * Генерация случайного номера карты (16 цифр).
     *
     * @return сгенерированный номер карты
     */
    private String generateCardNumber() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}

