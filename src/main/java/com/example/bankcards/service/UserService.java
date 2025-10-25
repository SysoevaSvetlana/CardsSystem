package com.example.bankcards.service;

import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.EmailAlreadyExistsException;
import com.example.bankcards.exception.UserNotFoundException;
import com.example.bankcards.exception.UsernameAlreadyExistsException;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Сервис для управления пользователями.
 * Обеспечивает создание, поиск, обновление и удаление пользователей.
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;

    /**
     * Сохранение пользователя
     *
     * @return сохраненный пользователь
     */
    public User save(User user) {
        return repository.save(user);
    }


    /**
     * Создание пользователя
     *
     * @return созданный пользователь
     */
    public User create(User user) {
        if (repository.existsByUsername(user.getUsername())) {
            throw new UsernameAlreadyExistsException(user.getUsername());
        }

        if (repository.existsByEmail(user.getEmail())) {
            throw new EmailAlreadyExistsException(user.getEmail());
        }

        return save(user);
    }

    /**
     * Получение пользователя по имени пользователя
     *
     * @return пользователь
     */
    public User getByUsername(String username) {
        return repository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Пользователь не найден"));

    }

    /**
     * Получение пользователя по имени пользователя
     * <p>
     * Нужен для Spring Security
     *
     * @return пользователь
     */
    public UserDetailsService userDetailsService() {
        return this::getByUsername;
    }

    /**
     * Получение текущего пользователя
     *
     * @return текущий пользователь
     */
    public User getCurrentUser() {
        // Получение имени пользователя из контекста Spring Security
        var username = SecurityContextHolder.getContext().getAuthentication().getName();
        return getByUsername(username);
    }


    /**
     * Выдача прав администратора текущему пользователю.
     *
     * @deprecated Используйте {@link #assignRole(Long, Role)} вместо этого метода
     */
    @Deprecated
    public void getAdmin() {
        var user = getCurrentUser();
        user.setRole(Role.ROLE_ADMIN);
        save(user);
    }

    /**
     * Получение всех пользователей (постранично).
     *
     * @param pageable параметры пагинации
     * @return страница пользователей
     */
    public Page<User> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    /**
     * Назначение роли пользователю.
     *
     * @param userId ID пользователя
     * @param role новая роль
     * @return обновленный пользователь
     * @throws UserNotFoundException если пользователь не найден
     */
    public User assignRole(Long userId, Role role) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        user.setRole(role);
        return repository.save(user);
    }

    /**
     * Удаление пользователя.
     *
     * @param userId ID пользователя
     * @throws UserNotFoundException если пользователь не найден
     */
    public void deleteUser(Long userId) {
        if (!repository.existsById(userId)) {
            throw new UserNotFoundException(userId);
        }
        repository.deleteById(userId);
    }

}
