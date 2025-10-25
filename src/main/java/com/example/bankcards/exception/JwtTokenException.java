package com.example.bankcards.exception;

public class JwtTokenException extends RuntimeException {
    public JwtTokenException(String message) { super(message); }
}
