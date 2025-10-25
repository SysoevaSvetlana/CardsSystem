package com.example.bankcards.exception;

// Дублирующий email
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("Пользователь с email '" + email + "' уже существует");
    }
}
