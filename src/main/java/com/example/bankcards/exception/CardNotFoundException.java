package com.example.bankcards.exception;


public class CardNotFoundException extends CardException {
    public CardNotFoundException(Long cardId) {
        super("Карта с ID " + cardId + " не найдена");
    }
}