package com.example.bankcards.exception;

// Карта не активна
public class CardNotActiveException extends CardException {
    public CardNotActiveException(Long cardId) {
        super("Карта с ID " + cardId + " не активна");
    }
}

