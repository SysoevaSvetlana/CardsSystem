package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequestDto;
import com.example.bankcards.dto.TransferResponseDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardException;
import com.example.bankcards.exception.CardNotActiveException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidTransferAmountException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import com.example.bankcards.security.CardNumberEncryptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сервис для выполнения переводов между картами.
 * Обеспечивает транзакционную безопасность при переводе средств.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final CardRepository cardRepository;
    private final CardNumberEncryptor cardNumberEncryptor;

    /**
     * Выполнение перевода средств между картами.
     * Использует pessimistic locking для предотвращения race conditions при параллельных переводах.
     * Проверяет, что обе карты принадлежат текущему пользователю.
     *
     * @param currentUser текущий аутентифицированный пользователь
     * @param dto данные перевода
     * @return результат перевода
     * @throws InvalidTransferAmountException если сумма перевода некорректна
     * @throws CardNotFoundException если одна из карт не найдена
     * @throws CardNotActiveException если одна из карт не активна
     * @throws InsufficientFundsException если недостаточно средств на карте отправителя
     * @throws CardException если попытка перевода с/на чужую карту
     */
    @Transactional
    public TransferResponseDto transferBetweenCards(User currentUser, TransferRequestDto dto) {
        BigDecimal amount = dto.getAmount();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferAmountException();
        }

        // Используем pessimistic locking для предотвращения race conditions
        // Блокируем строки в БД до завершения транзакции
        // Важно: блокируем карты в определенном порядке (по ID) для предотвращения deadlock
        Long fromCardId = dto.getFromCardId();
        Long toCardId = dto.getToCardId();

        Card fromCard;
        Card toCard;

        // Блокируем карты в порядке возрастания ID для предотвращения deadlock
        if (fromCardId < toCardId) {
            fromCard = cardRepository.findByIdWithLock(fromCardId)
                    .orElseThrow(() -> new CardNotFoundException(fromCardId));
            toCard = cardRepository.findByIdWithLock(toCardId)
                    .orElseThrow(() -> new CardNotFoundException(toCardId));
        } else {
            toCard = cardRepository.findByIdWithLock(toCardId)
                    .orElseThrow(() -> new CardNotFoundException(toCardId));
            fromCard = cardRepository.findByIdWithLock(fromCardId)
                    .orElseThrow(() -> new CardNotFoundException(fromCardId));
        }

        // КРИТИЧЕСКАЯ ПРОВЕРКА БЕЗОПАСНОСТИ: обе карты должны принадлежать текущему пользователю
        if (!fromCard.getUser().getId().equals(currentUser.getId())) {
            log.warn("Security violation: User {} attempted to transfer from card {} owned by user {}",
                currentUser.getId(), fromCard.getId(), fromCard.getUser().getId());
            throw new CardException("Нельзя переводить средства с чужой карты");
        }
        if (!toCard.getUser().getId().equals(currentUser.getId())) {
            log.warn("Security violation: User {} attempted to transfer to card {} owned by user {}",
                currentUser.getId(), toCard.getId(), toCard.getUser().getId());
            throw new CardException("Нельзя переводить средства на чужую карту");
        }

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardNotActiveException(fromCard.getId());
        }
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardNotActiveException(toCard.getId());
        }

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(fromCard.getId());
        }

        // Логирование перевода для аудита
        log.info("Transfer initiated: user={}, fromCard={}, toCard={}, amount={}",
            currentUser.getId(), fromCardId, toCardId, amount);

        fromCard.setBalance(fromCard.getBalance().subtract(amount));
        toCard.setBalance(toCard.getBalance().add(amount));

        cardRepository.save(fromCard);
        cardRepository.save(toCard);

        Transfer transfer = new Transfer();
        transfer.setFromCard(fromCard);
        transfer.setToCard(toCard);
        transfer.setAmount(amount);
        transfer.setStatus("SUCCESS");
        transfer.setCreatedAt(LocalDateTime.now());
        transferRepository.save(transfer);

        log.info("Transfer completed successfully: transferId={}, user={}, amount={}",
            transfer.getId(), currentUser.getId(), amount);

        return mapToResponse(transfer);
    }

    /**
     * Преобразование сущности Transfer в DTO.
     *
     * @param transfer сущность перевода
     * @return DTO перевода
     */
    private TransferResponseDto mapToResponse(Transfer transfer) {
        TransferResponseDto dto = new TransferResponseDto();
        dto.setId(transfer.getId());

        // Получаем замаскированные номера карт
        dto.setFromCardMaskedNumber(getMaskedNumber(transfer.getFromCard()));
        dto.setToCardMaskedNumber(getMaskedNumber(transfer.getToCard()));

        dto.setAmount(transfer.getAmount());
        dto.setStatus(transfer.getStatus());
        dto.setCreatedAt(transfer.getCreatedAt());
        return dto;
    }

    /**
     * Получение замаскированного номера карты.
     *
     * @param card карта
     * @return замаскированный номер
     */
    private String getMaskedNumber(Card card) {
        String maskedNumber = card.getMaskedNumber();
        if (maskedNumber == null && card.getEncryptedNumber() != null) {
            String decrypted = cardNumberEncryptor.decrypt(card.getEncryptedNumber());
            maskedNumber = cardNumberEncryptor.mask(decrypted);
        }
        return maskedNumber;
    }
}
