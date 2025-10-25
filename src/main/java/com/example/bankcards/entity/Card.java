package com.example.bankcards.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.bankcards.security.CardNumberEncryptor;

/**
 * Сущность банковской карты.
 * Хранит зашифрованный номер карты, баланс, статус и связи с пользователем и переводами.
 */
@Data
@Entity
@Table(name = "card")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "encrypted_number", nullable = false, unique = true)
    @NotNull
    @Pattern(regexp = "\\d{16}", message = "Номер карты должен содержать ровно 16 цифр")
    private String encryptedNumber;

    @Transient
    private String maskedNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDate expiryDate;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @OneToMany(mappedBy = "fromCard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transfer> outgoingTransfers = new ArrayList<>();

    @OneToMany(mappedBy = "toCard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transfer> incomingTransfers = new ArrayList<>();

    /**
     * Установка номера карты с шифрованием.
     *
     * @param plainNumber незашифрованный номер карты
     */
    public void setCardNumber(String plainNumber) {
        this.encryptedNumber = CardNumberEncryptor.encrypt(plainNumber);
        this.maskedNumber = CardNumberEncryptor.mask(plainNumber);
    }

    /**
     * Получение расшифрованного номера карты.
     *
     * @return расшифрованный номер карты
     */
    public String getDecryptedNumber() {
        return CardNumberEncryptor.decrypt(this.encryptedNumber);
    }

    /**
     * Получение замаскированного номера карты.
     *
     * @return замаскированный номер карты (например, **** **** **** 1234)
     */
    public String getMaskedNumber() {
        if (maskedNumber == null && encryptedNumber != null) {
            maskedNumber = CardNumberEncryptor.mask(getDecryptedNumber());
        }
        return maskedNumber;
    }
}
