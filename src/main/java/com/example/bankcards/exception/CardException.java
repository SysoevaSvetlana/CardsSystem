package com.example.bankcards.exception;

// Общий базовый класс для ошибок карты
public class CardException extends RuntimeException {
    public CardException(String message) { super(message); }
}
