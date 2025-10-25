package com.example.bankcards.exception;

// Недостаточно средств
public class InsufficientFundsException extends CardException {
    public InsufficientFundsException(Long cardId) {
        super("Недостаточно средств на карте с ID " + cardId);
    }
}

