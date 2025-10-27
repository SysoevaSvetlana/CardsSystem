package com.example.bankcards.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Сущность банковской карты.
 * Хранит зашифрованный номер карты, баланс, статус и связи с пользователем и переводами.
 *
 * Примечание: Методы шифрования/дешифрования вынесены в CardService,
 * так как JPA Entity не должна зависеть от Spring beans.
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
}
