package com.example.bankcards.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Spring Bean для шифрования, дешифрования и маскировки номеров банковских карт.
 * Использует современный алгоритм AES-256-GCM вместо устаревшего PBEWithMD5AndDES.
 *
 * AES-256-GCM обеспечивает:
 * - Конфиденциальность (шифрование)
 * - Аутентичность (защита от подделки)
 * - Целостность (защита от изменения)
 */
@Slf4j
@Component
public class CardNumberEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final int GCM_IV_LENGTH = 12; // 12 bytes (96 bits) - рекомендовано для GCM

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    /**
     * Конструктор с инъекцией пароля из Spring properties.
     *
     * @param password пароль для генерации ключа шифрования
     */
    public CardNumberEncryptor(@Value("${app.security.encryption.password}") String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalStateException(
                "Encryption password is not configured. Set 'app.security.encryption.password' property."
            );
        }

        this.secretKey = deriveKeyFromPassword(password);
        this.secureRandom = new SecureRandom();

        log.info("CardNumberEncryptor initialized with AES-256-GCM algorithm");
    }

    /**
     * Генерация 256-битного ключа из пароля с использованием SHA-256.
     *
     * @param password пароль
     * @return секретный ключ
     */
    private SecretKey deriveKeyFromPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive encryption key from password", e);
        }
    }

    /**
     * Шифрование номера карты.
     *
     * @param cardNumber незашифрованный номер карты
     * @return зашифрованный номер карты
     * @throws RuntimeException если произошла ошибка шифрования
     */
    public String encrypt(String cardNumber) {
        if (cardNumber == null || cardNumber.isEmpty()) {
            throw new IllegalArgumentException("Card number cannot be null or empty");
        }

        try {
            // Генерация случайного IV (Initialization Vector)
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            // Инициализация шифра
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // Шифрование
            byte[] plaintext = cardNumber.getBytes(StandardCharsets.UTF_8);
            byte[] ciphertext = cipher.doFinal(plaintext);

            // Объединение IV и зашифрованных данных
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            // Кодирование в Base64 для хранения в БД
            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            log.error("Error encrypting card number", e);
            throw new RuntimeException("Failed to encrypt card number", e);
        }
    }

    /**
     * Дешифрование номера карты.
     *
     * @param encrypted зашифрованный номер карты
     * @return расшифрованный номер карты
     * @throws RuntimeException если произошла ошибка дешифрования
     */
    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            throw new IllegalArgumentException("Encrypted data cannot be null or empty");
        }

        try {
            // Декодирование из Base64
            byte[] decodedData = Base64.getDecoder().decode(encrypted);

            // Извлечение IV и зашифрованных данных
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedData);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            // Инициализация шифра для дешифрования
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // Дешифрование
            byte[] plaintext = cipher.doFinal(ciphertext);

            return new String(plaintext, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Error decrypting card number", e);
            throw new RuntimeException("Failed to decrypt card number", e);
        }
    }

    /**
     * Маскировка номера карты для безопасного отображения.
     *
     * @param decryptedNumber расшифрованный номер карты
     * @return замаскированный номер (например, **** **** **** 1234)
     */
    public String mask(String decryptedNumber) {
        if (decryptedNumber == null || decryptedNumber.length() < 4) {
            return "****";
        }

        String last4 = decryptedNumber.substring(decryptedNumber.length() - 4);
        return "**** **** **** " + last4;
    }
}
