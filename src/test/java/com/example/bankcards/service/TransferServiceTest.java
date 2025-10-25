package com.example.bankcards.service;

import com.example.bankcards.dto.TransferRequestDto;
import com.example.bankcards.dto.TransferResponseDto;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardNotActiveException;
import com.example.bankcards.exception.CardNotFoundException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.InvalidTransferAmountException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferService Unit Tests")
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private TransferService transferService;

    private User user1;
    private User user2;
    private Card fromCard;
    private Card toCard;
    private TransferRequestDto transferRequest;

    @BeforeAll
    static void setUpEnvironment() {
        // Устанавливаем тестовый пароль для Jasypt
        // Это необходимо для работы CardNumberEncryptor
        System.setProperty("JASYPT_PASSWORD", "test-password-for-unit-tests");
    }

    @BeforeEach
    void setUp() {
        // Создаем пользователей
        user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");

        user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");

        // Создаем карту отправителя
        fromCard = new Card();
        fromCard.setId(1L);
        fromCard.setCardNumber("1234567812345678");
        fromCard.setBalance(new BigDecimal("1000.00"));
        fromCard.setStatus(CardStatus.ACTIVE);
        fromCard.setUser(user1);
        fromCard.setExpiryDate(LocalDate.now().plusYears(2));

        // Создаем карту получателя
        toCard = new Card();
        toCard.setId(2L);
        toCard.setCardNumber("8765432187654321");
        toCard.setBalance(new BigDecimal("500.00"));
        toCard.setStatus(CardStatus.ACTIVE);
        toCard.setUser(user2);
        toCard.setExpiryDate(LocalDate.now().plusYears(2));

        // Создаем запрос на перевод
        transferRequest = new TransferRequestDto();
        transferRequest.setFromCardId(1L);
        transferRequest.setToCardId(2L);
        transferRequest.setAmount(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("Успешный перевод между картами")
    void transferBetweenCards_Success() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));


        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            transfer.setId(1L);
            return transfer;
        });

        // Act
        TransferResponseDto result = transferService.transferBetweenCards(transferRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(result.getStatus()).isEqualTo("SUCCESS");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getFromCardMaskedNumber()).isNotNull();
        assertThat(result.getToCardMaskedNumber()).isNotNull();

        // Проверяем, что балансы изменились
        assertThat(fromCard.getBalance()).isEqualByComparingTo(new BigDecimal("900.00"));
        assertThat(toCard.getBalance()).isEqualByComparingTo(new BigDecimal("600.00"));

        // Проверяем, что карты были сохранены
        verify(cardRepository, times(1)).save(fromCard);
        verify(cardRepository, times(1)).save(toCard);
        verify(transferRepository, times(1)).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Перевод с нулевой суммой - должно выбросить InvalidTransferAmountException")
    void transferBetweenCards_ZeroAmount_ThrowsException() {
        // Arrange
        transferRequest.setAmount(BigDecimal.ZERO);

        // Act & Assert
        assertThatThrownBy(() -> transferService.transferBetweenCards(transferRequest))
                .isInstanceOf(InvalidTransferAmountException.class)
                .hasMessageContaining("Сумма перевода должна быть больше нуля");

        // Проверяем, что никакие операции с БД не выполнялись
        verify(cardRepository, never()).findById(any());
        verify(cardRepository, never()).save(any());
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Перевод с отрицательной суммой - должно выбросить InvalidTransferAmountException")
    void transferBetweenCards_NegativeAmount_ThrowsException() {
        // Arrange
        transferRequest.setAmount(new BigDecimal("-50.00"));

        // Act & Assert
        assertThatThrownBy(() -> transferService.transferBetweenCards(transferRequest))
                .isInstanceOf(InvalidTransferAmountException.class);

        verify(cardRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Перевод с несуществующей карты отправителя - должно выбросить CardNotFoundException")
    void transferBetweenCards_FromCardNotFound_ThrowsException() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transferService.transferBetweenCards(transferRequest))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("Карта с ID 1 не найдена");

        verify(cardRepository, times(1)).findById(1L);
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Перевод на несуществующую карту получателя - должно выбросить CardNotFoundException")
    void transferBetweenCards_ToCardNotFound_ThrowsException() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> transferService.transferBetweenCards(transferRequest))
                .isInstanceOf(CardNotFoundException.class)
                .hasMessageContaining("Карта с ID 2 не найдена");

        verify(cardRepository, times(1)).findById(1L);
        verify(cardRepository, times(1)).findById(2L);
        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Перевод с заблокированной карты отправителя - должно выбросить CardNotActiveException")
    void transferBetweenCards_FromCardBlocked_ThrowsException() {
        // Arrange
        fromCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        // Act & Assert
        assertThatThrownBy(() -> transferService.transferBetweenCards(transferRequest))
                .isInstanceOf(CardNotActiveException.class)
                .hasMessageContaining("Карта с ID 1 не активна");

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Перевод на заблокированную карту получателя - должно выбросить CardNotActiveException")
    void transferBetweenCards_ToCardBlocked_ThrowsException() {
        // Arrange
        toCard.setStatus(CardStatus.BLOCKED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        // Act & Assert
        assertThatThrownBy(() -> transferService.transferBetweenCards(transferRequest))
                .isInstanceOf(CardNotActiveException.class)
                .hasMessageContaining("Карта с ID 2 не активна");

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Перевод с карты со статусом BLOCK_REQUESTED - должно выбросить CardNotActiveException")
    void transferBetweenCards_FromCardBlockRequested_ThrowsException() {
        // Arrange
        fromCard.setStatus(CardStatus.BLOCK_REQUESTED);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        // Act & Assert
        assertThatThrownBy(() -> transferService.transferBetweenCards(transferRequest))
                .isInstanceOf(CardNotActiveException.class);

        verify(cardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Перевод при недостаточном балансе - должно выбросить InsufficientFundsException")
    void transferBetweenCards_InsufficientFunds_ThrowsException() {
        // Arrange
        transferRequest.setAmount(new BigDecimal("1500.00")); // Больше чем баланс (1000)
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        // Act & Assert
        assertThatThrownBy(() -> transferService.transferBetweenCards(transferRequest))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Недостаточно средств на карте с ID 1");

        // Проверяем, что балансы не изменились
        assertThat(fromCard.getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(toCard.getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
        
        verify(cardRepository, never()).save(any());
        verify(transferRepository, never()).save(any());
    }

    @Test
    @DisplayName("Перевод всего баланса - граничный случай")
    void transferBetweenCards_TransferAllBalance_Success() {
        // Arrange
        transferRequest.setAmount(new BigDecimal("1000.00")); // Весь баланс
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            transfer.setId(1L);
            return transfer;
        });

        // Act
        TransferResponseDto result = transferService.transferBetweenCards(transferRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(fromCard.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(toCard.getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));

        verify(cardRepository, times(2)).save(any(Card.class));
        verify(transferRepository, times(1)).save(any(Transfer.class));
    }

    @Test
    @DisplayName("Проверка сохранения Transfer с правильными данными")
    void transferBetweenCards_TransferEntitySavedCorrectly() {
        // Arrange
        when(cardRepository.findById(1L)).thenReturn(Optional.of(fromCard));
        when(cardRepository.findById(2L)).thenReturn(Optional.of(toCard));

        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);
        when(transferRepository.save(transferCaptor.capture())).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            transfer.setId(1L);
            return transfer;
        });

        // Act
        transferService.transferBetweenCards(transferRequest);

        // Assert
        Transfer capturedTransfer = transferCaptor.getValue();
        assertThat(capturedTransfer.getFromCard()).isEqualTo(fromCard);
        assertThat(capturedTransfer.getToCard()).isEqualTo(toCard);
        assertThat(capturedTransfer.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(capturedTransfer.getStatus()).isEqualTo("SUCCESS");
        assertThat(capturedTransfer.getCreatedAt()).isNotNull();
    }
}

