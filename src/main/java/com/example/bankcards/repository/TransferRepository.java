package com.example.bankcards.repository;

import com.example.bankcards.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Репозиторий для работы с сущностью Transfer.
 * Предоставляет методы для поиска переводов по пользователю.
 */
@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    /**
     * Поиск всех исходящих переводов пользователя.
     *
     * @param userId ID пользователя
     * @return список исходящих переводов
     */
    List<Transfer> findByFromCardUserId(Long userId);

    /**
     * Поиск всех входящих переводов пользователя.
     *
     * @param userId ID пользователя
     * @return список входящих переводов
     */
    List<Transfer> findByToCardUserId(Long userId);
}