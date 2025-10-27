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
import com.example.bankcards.security.CardNumberEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис для управления банковскими картами.
 * Обеспечивает создание, активацию, блокировку и удаление карт.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardNumberEncryptor cardNumberEncryptor;
    private final SecureRandom secureRandom = new SecureRandom();

    // BIN (Bank Identification Number) - первые 6 цифр карты
    private static final String BANK_BIN = "400000"; // Visa test BIN
    private static final int MAX_GENERATION_ATTEMPTS = 10;

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
        // Шифруем номер карты через сервис
        card.setEncryptedNumber(cardNumberEncryptor.encrypt(generatedNumber));
        card.setMaskedNumber(cardNumberEncryptor.mask(generatedNumber));

        Card saved = cardRepository.save(card);
        return mapToResponse(saved);
    }

    /**
     * Запрос блокировки карты пользователем.
     *
     * @param currentUser текущий пользователь
     * @param dto данные запроса блокировки
     * @return обновленная карта
     * @throws CardNotFoundException если карта не найдена
     * @throws CardException если карта уже заблокирована или принадлежит другому пользователю
     */
    public CardResponseDto requestBlock(User currentUser, CardBlockRequestDto dto) {
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
     * Проверяет:
     * - Существование карты
     * - Баланс карты должен быть равен 0
     * - Отсутствие связанных переводов (входящих и исходящих)
     *
     * @param cardId ID карты
     * @throws CardNotFoundException если карта не найдена
     * @throws CardException если карту нельзя удалить (ненулевой баланс или есть переводы)
     */
    @Transactional
    public void deleteCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new CardNotFoundException(cardId));

        // Проверка баланса
        if (card.getBalance().compareTo(BigDecimal.ZERO) > 0) {
            throw new CardException(
                String.format("Невозможно удалить карту с ненулевым балансом. Текущий баланс: %.2f",
                    card.getBalance())
            );
        }

        // Проверка наличия переводов
        if (!card.getOutgoingTransfers().isEmpty() || !card.getIncomingTransfers().isEmpty()) {
            throw new CardException(
                "Невозможно удалить карту с историей переводов. " +
                "Переводов исходящих: " + card.getOutgoingTransfers().size() + ", " +
                "входящих: " + card.getIncomingTransfers().size()
            );
        }

        cardRepository.delete(card);
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
     * @param currentUser текущий пользователь
     * @param cardId ID карты
     * @return баланс карты
     * @throws CardNotFoundException если карта не найдена
     * @throws CardException если карта принадлежит другому пользователю
     */
    public BigDecimal getCardBalance(User currentUser, Long cardId) {
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
     * @param currentUser текущий пользователь
     * @return список карт пользователя
     */
    public List<CardResponseDto> getUserCards(User currentUser) {
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

        // Если maskedNumber не установлен, генерируем его из зашифрованного номера
        String maskedNumber = card.getMaskedNumber();
        if (maskedNumber == null && card.getEncryptedNumber() != null) {
            String decrypted = cardNumberEncryptor.decrypt(card.getEncryptedNumber());
            maskedNumber = cardNumberEncryptor.mask(decrypted);
        }
        dto.setMaskedNumber(maskedNumber);

        dto.setBalance(card.getBalance());
        dto.setStatus(card.getStatus().name());
        dto.setExpiryDate(card.getExpiryDate());
        dto.setUserId(card.getUser().getId());
        return dto;
    }

    /**
     * Генерация случайного номера карты (16 цифр) с использованием алгоритма Луна.
     *
     * Использует:
     * - SecureRandom для криптографически стойкой генерации
     * - BIN (Bank Identification Number) для идентификации банка
     * - Алгоритм Луна для валидации номера карты
     * - Проверку уникальности в базе данных
     *
     * @return сгенерированный и валидный номер карты
     * @throws CardException если не удалось сгенерировать уникальный номер
     */
    private String generateCardNumber() {
        // Получаем все существующие зашифрованные номера карт
        List<String> existingEncryptedNumbers = cardRepository.findAllEncryptedNumbers();

        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String cardNumber = generateCardNumberWithLuhn();

            // Проверка уникальности: расшифровываем все существующие номера и сравниваем
            boolean isDuplicate = existingEncryptedNumbers.stream()
                    .anyMatch(encryptedNumber -> {
                        try {
                            String decryptedNumber = cardNumberEncryptor.decrypt(encryptedNumber);
                            return cardNumber.equals(decryptedNumber);
                        } catch (Exception e) {
                            log.error("Ошибка при расшифровке номера карты для проверки уникальности", e);
                            return false;
                        }
                    });

            if (!isDuplicate) {
                return cardNumber;
            }

            log.warn("Сгенерирован дубликат номера карты, попытка {}/{}", attempt + 1, MAX_GENERATION_ATTEMPTS);
        }

        throw new CardException("Не удалось сгенерировать уникальный номер карты после " + MAX_GENERATION_ATTEMPTS + " попыток");
    }

    /**
     * Генерация номера карты с контрольной суммой по алгоритму Луна.
     *
     * Формат: BIN (6 цифр) + Account Number (9 цифр) + Check Digit (1 цифра)
     *
     * @return номер карты из 16 цифр, проходящий проверку Луна
     */
    private String generateCardNumberWithLuhn() {
        StringBuilder sb = new StringBuilder();

        // Добавляем BIN (первые 6 цифр)
        sb.append(BANK_BIN);

        // Генерируем 9 случайных цифр для номера счета
        for (int i = 0; i < 9; i++) {
            sb.append(secureRandom.nextInt(10));
        }

        // Вычисляем контрольную цифру по алгоритму Луна
        int checkDigit = calculateLuhnCheckDigit(sb.toString());
        sb.append(checkDigit);

        return sb.toString();
    }

    /**
     * Вычисление контрольной цифры по алгоритму Луна (Luhn algorithm).

     * @param cardNumberWithoutCheckDigit номер карты без контрольной цифры (15 цифр)
     * @return контрольная цифра (0-9)
     */
    private int calculateLuhnCheckDigit(String cardNumberWithoutCheckDigit) {
        int sum = 0;
        boolean alternate = true; // Начинаем с true, т.к. идем справа налево

        // Проходим по цифрам справа налево
        for (int i = cardNumberWithoutCheckDigit.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(cardNumberWithoutCheckDigit.charAt(i));

            if (alternate) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            alternate = !alternate;
        }

        return (10 - (sum % 10)) % 10;
    }
}

