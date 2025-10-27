package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с сущностью Card.
 * Предоставляет методы для поиска карт по пользователю.
 */
@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    /**
     * Поиск всех карт пользователя.
     *
     * @param userId ID пользователя
     * @return список карт пользователя
     */
    List<Card> findByUserId(Long userId);

    /**
     * Поиск карт пользователя с пагинацией.
     *
     * @param userId ID пользователя
     * @param pageable параметры пагинации
     * @return страница карт пользователя
     */
    Page<Card> findByUserId(Long userId, Pageable pageable);

    /**
     * Получение всех карт с пагинацией.
     *
     * @param pageable параметры пагинации
     * @return страница всех карт
     */
    Page<Card> findAll(Pageable pageable);

    /**
     * Получение всех зашифрованных номеров карт.
     *
     * @return список всех зашифрованных номеров
     */
    @Query("SELECT c.encryptedNumber FROM Card c")
    List<String> findAllEncryptedNumbers();

    /**
     * Поиск карты по ID с пессимистической блокировкой для записи.
     * Используется при переводах для предотвращения race conditions.
     * Блокирует строку в БД до завершения транзакции.
     *
     * @param id ID карты
     * @return Optional с картой, если найдена
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Card c WHERE c.id = :id")
    Optional<Card> findByIdWithLock(@Param("id") Long id);
}
