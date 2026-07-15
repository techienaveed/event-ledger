package com.sample.account.service;

import com.sample.account.dto.AccountResponse;
import com.sample.account.dto.BalanceResponse;
import com.sample.account.dto.TransactionRequest;
import com.sample.account.entity.Account;
import com.sample.account.entity.TransactionEntity;
import com.sample.account.exception.AccountNotFoundException;
import com.sample.account.repository.AccountRepository;
import com.sample.account.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AccountServiceImpl accountService;

    private String accountId;
    private String eventId;
    private BigDecimal amount;
    private Instant now;

    @BeforeEach
    void setUp() {
        accountId = "ACC001";
        eventId = "EVT001";
        amount = new BigDecimal("100.00");
        now = Instant.now();
    }

    @Test
    void testApplyTransactionCredit_NewAccount() {
        // Arrange
        TransactionRequest request = new TransactionRequest(
                eventId,
                accountId,
                "CREDIT",
                amount,
                "USD",
                now
        );

        when(transactionRepository.findByEventId(eventId)).thenReturn(Optional.empty());
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        accountService.applyTransaction(request);

        // Assert
        verify(accountRepository).save(argThat(account ->
                account.getAccountId().equals(accountId) &&
                        account.getBalance().compareTo(amount) == 0
        ));
        verify(transactionRepository).save(argThat(transaction ->
                transaction.getEventId().equals(eventId) &&
                        transaction.getType().equals("CREDIT") &&
                        transaction.getAmount().compareTo(amount) == 0
        ));
    }

    @Test
    void testApplyTransactionCredit_ExistingAccount() {
        // Arrange
        BigDecimal existingBalance = new BigDecimal("50.00");
        BigDecimal expectedBalance = existingBalance.add(amount);

        Account existingAccount = Account.builder()
                .accountId(accountId)
                .balance(existingBalance)
                .updatedAt(now)
                .build();

        TransactionRequest request = new TransactionRequest(
                eventId,
                accountId,
                "CREDIT",
                amount,
                "USD",
                now
        );

        when(transactionRepository.findByEventId(eventId)).thenReturn(Optional.empty());
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(existingAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        accountService.applyTransaction(request);

        // Assert
        verify(accountRepository).save(argThat(account ->
                account.getBalance().compareTo(expectedBalance) == 0
        ));
    }

    @Test
    void testApplyTransactionDebit_ExistingAccount() {
        // Arrange
        BigDecimal existingBalance = new BigDecimal("200.00");
        BigDecimal expectedBalance = existingBalance.subtract(amount);

        Account existingAccount = Account.builder()
                .accountId(accountId)
                .balance(existingBalance)
                .updatedAt(now)
                .build();

        TransactionRequest request = new TransactionRequest(
                eventId,
                accountId,
                "DEBIT",
                amount,
                "USD",
                now
        );

        when(transactionRepository.findByEventId(eventId)).thenReturn(Optional.empty());
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(existingAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        accountService.applyTransaction(request);

        // Assert
        verify(accountRepository).save(argThat(account ->
                account.getBalance().compareTo(expectedBalance) == 0
        ));
    }

    @Test
    void testApplyTransaction_DuplicateEventId() {
        // Arrange
        TransactionRequest request = new TransactionRequest(
                eventId,
                accountId,
                "CREDIT",
                amount,
                "USD",
                now
        );

        TransactionEntity existingTransaction = TransactionEntity.builder()
                .eventId(eventId)
                .accountId(accountId)
                .type("CREDIT")
                .amount(amount)
                .currency("USD")
                .eventTimestamp(now)
                .build();

        when(transactionRepository.findByEventId(eventId)).thenReturn(Optional.of(existingTransaction));

        // Act
        accountService.applyTransaction(request);

        // Assert
        verify(accountRepository, never()).save(any(Account.class));
        verify(transactionRepository, never()).save(any(TransactionEntity.class));
    }

    @Test
    void testGetBalance_Success() {
        // Arrange
        Account account = Account.builder()
                .accountId(accountId)
                .balance(amount)
                .updatedAt(now)
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // Act
        BalanceResponse response = accountService.getBalance(accountId);

        // Assert
        assertNotNull(response);
        assertEquals(accountId, response.accountId());
        assertEquals(amount, response.balance());
    }

    @Test
    void testGetBalance_AccountNotFound() {
        // Arrange
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> accountService.getBalance(accountId));
    }

    @Test
    void testGetAccount_Success() {
        // Arrange
        Account account = Account.builder()
                .accountId(accountId)
                .balance(amount)
                .updatedAt(now)
                .build();

        TransactionEntity transaction = TransactionEntity.builder()
                .eventId(eventId)
                .accountId(accountId)
                .type("CREDIT")
                .amount(amount)
                .currency("USD")
                .eventTimestamp(now)
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccountIdOrderByEventTimestampAsc(accountId))
                .thenReturn(List.of(transaction));

        // Act
        AccountResponse response = accountService.getAccount(accountId);

        // Assert
        assertNotNull(response);
        assertEquals(accountId, response.accountId());
        assertEquals(amount, response.balance());
        assertEquals(1, response.transactions().size());
        assertEquals(eventId, response.transactions().get(0).eventId());
    }

    @Test
    void testGetAccount_NoTransactions() {
        // Arrange
        Account account = Account.builder()
                .accountId(accountId)
                .balance(BigDecimal.ZERO)
                .updatedAt(now)
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(transactionRepository.findByAccountIdOrderByEventTimestampAsc(accountId))
                .thenReturn(List.of());

        // Act
        AccountResponse response = accountService.getAccount(accountId);

        // Assert
        assertNotNull(response);
        assertEquals(accountId, response.accountId());
        assertEquals(BigDecimal.ZERO, response.balance());
        assertTrue(response.transactions().isEmpty());
    }

    @Test
    void testGetAccount_AccountNotFound() {
        // Arrange
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> accountService.getAccount(accountId));
    }

    @Test
    void testApplyTransaction_DebitReducesBalance() {
        // Arrange
        BigDecimal existingBalance = new BigDecimal("150.00");
        BigDecimal debitAmount = new BigDecimal("50.00");
        BigDecimal expectedBalance = existingBalance.subtract(debitAmount);

        Account existingAccount = Account.builder()
                .accountId(accountId)
                .balance(existingBalance)
                .updatedAt(now)
                .build();

        TransactionRequest request = new TransactionRequest(
                eventId,
                accountId,
                "DEBIT",
                debitAmount,
                "USD",
                now
        );

        when(transactionRepository.findByEventId(eventId)).thenReturn(Optional.empty());
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(existingAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(TransactionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        accountService.applyTransaction(request);

        // Assert
        verify(accountRepository).save(argThat(account ->
                account.getBalance().compareTo(expectedBalance) == 0
        ));
    }
}
