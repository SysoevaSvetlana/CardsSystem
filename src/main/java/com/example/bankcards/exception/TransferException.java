package com.example.bankcards.exception;

// Общий класс для ошибок перевода
public class TransferException extends RuntimeException {
    public TransferException(String message) { super(message); }
}