package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}
