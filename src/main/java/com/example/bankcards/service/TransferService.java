package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequestDto;
import com.example.bankcards.dto.TransferResponseDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.exception.CardNotActiveException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidTransferAmountException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Сервис для выполнения переводов между картами.
 * Обеспечивает транзакционную безопасность при переводе средств.
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final CardRepository cardRepository;

    /**
     * Выполнение перевода средств между картами.
     *
     * @param dto данные перевода
     * @return результат перевода
     * @throws InvalidTransferAmountException если сумма перевода некорректна
     * @throws CardNotFoundException если одна из карт не найдена
     * @throws CardNotActiveException если одна из карт не активна
     * @throws InsufficientFundsException если недостаточно средств на карте отправителя
     */
    @Transactional
    public TransferResponseDto transferBetweenCards(TransferRequestDto dto) {
        BigDecimal amount = dto.getAmount();

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferAmountException();
        }

        Card fromCard = cardRepository.findById(dto.getFromCardId())
                .orElseThrow(() -> new CardNotFoundException(dto.getFromCardId()));
        Card toCard = cardRepository.findById(dto.getToCardId())
                .orElseThrow(() -> new CardNotFoundException(dto.getToCardId()));

        if (fromCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardNotActiveException(fromCard.getId());
        }
        if (toCard.getStatus() != CardStatus.ACTIVE) {
            throw new CardNotActiveException(toCard.getId());
        }

        if (fromCard.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(fromCard.getId());
        }

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
        dto.setFromCardMaskedNumber(transfer.getFromCard().getMaskedNumber());
        dto.setToCardMaskedNumber(transfer.getToCard().getMaskedNumber());
        dto.setAmount(transfer.getAmount());
        dto.setStatus(transfer.getStatus());
        dto.setCreatedAt(transfer.getCreatedAt());
        return dto;
    }
}
