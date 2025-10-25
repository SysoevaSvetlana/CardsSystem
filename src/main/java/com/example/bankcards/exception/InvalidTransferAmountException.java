package com.example.bankcards.exception;

// Неверная сумма перевода
public class InvalidTransferAmountException extends TransferException {
    public InvalidTransferAmountException() {
        super("Сумма перевода должна быть больше нуля");
    }
}
