package com.example.bankcards.security;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;

/**
 * Утилитный класс для шифрования, дешифрования и маскировки номеров банковских карт.
 * Использует алгоритм PBEWithMD5AndDES для шифрования.
 */
public class CardNumberEncryptor {

    private static final String ALGORITHM = "PBEWithMD5AndDES";
    private static final String SECRET_KEY = getJasyptPassword();
    private static final StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();

    static {
        encryptor.setAlgorithm(ALGORITHM);
        encryptor.setPassword(SECRET_KEY);
    }

    /**
     * Получает пароль Jasypt из переменной окружения или системного свойства.
     * Приоритет: переменная окружения > системное свойство.
     *
     * @return пароль для шифрования
     * @throws IllegalStateException если пароль не установлен
     */
    private static String getJasyptPassword() {
        String password = System.getenv("JASYPT_PASSWORD");
        if (password == null || password.isEmpty()) {
            password = System.getProperty("JASYPT_PASSWORD");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalStateException(
                "JASYPT_PASSWORD не установлен. Установите переменную окружения или системное свойство."
            );
        }
        return password;
    }

    /**
     * Шифрование номера карты.
     *
     * @param cardNumber незашифрованный номер карты
     * @return зашифрованный номер карты
     * @throws RuntimeException если произошла ошибка шифрования
     */
    public static String encrypt(String cardNumber) {
        try {
            return encryptor.encrypt(cardNumber);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при шифровании номера карты", e);
        }
    }

    /**
     * Дешифрование номера карты.
     *
     * @param encrypted зашифрованный номер карты
     * @return расшифрованный номер карты
     * @throws RuntimeException если произошла ошибка дешифрования
     */
    public static String decrypt(String encrypted) {
        try {
            return encryptor.decrypt(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при расшифровке номера карты", e);
        }
    }

    /**
     * Маскировка номера карты для безопасного отображения.
     *
     * @param decryptedNumber расшифрованный номер карты
     * @return замаскированный номер (например, **** **** **** 1234)
     */
    public static String mask(String decryptedNumber) {
        if (decryptedNumber == null || decryptedNumber.length() < 4) {
            return "****";
        }
        String last4 = decryptedNumber.substring(decryptedNumber.length() - 4);
        return "**** **** **** " + last4;
    }
}
