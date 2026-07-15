package com.sample.account.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void testAccountBuilderDefaults() {
        // Arrange & Act
        Account account = Account.builder()
                .accountId("ACC001")
                .updatedAt(Instant.now())
                .build();

        // Assert
        assertEquals("ACC001", account.getAccountId());
        assertEquals(BigDecimal.ZERO, account.getBalance());
        assertNotNull(account.getUpdatedAt());
    }

    @Test
    void testAccountBuilder() {
        // Arrange
        String accountId = "ACC001";
        BigDecimal balance = new BigDecimal("100.00");
        Instant now = Instant.now();

        // Act
        Account account = Account.builder()
                .accountId(accountId)
                .balance(balance)
                .updatedAt(now)
                .build();

        // Assert
        assertEquals(accountId, account.getAccountId());
        assertEquals(balance, account.getBalance());
        assertEquals(now, account.getUpdatedAt());
    }

    @Test
    void testAccountSettersAndGetters() {
        // Arrange
        Account account = new Account();
        String accountId = "ACC002";
        BigDecimal balance = new BigDecimal("250.00");
        Instant now = Instant.now();

        // Act
        account.setAccountId(accountId);
        account.setBalance(balance);
        account.setUpdatedAt(now);

        // Assert
        assertEquals(accountId, account.getAccountId());
        assertEquals(balance, account.getBalance());
        assertEquals(now, account.getUpdatedAt());
    }

    @Test
    void testAccountNoArgsConstructor() {
        // Act
        Account account = new Account();

        // Assert
        assertNull(account.getAccountId());
        assertNull(account.getBalance());
        assertNull(account.getUpdatedAt());
    }

    @Test
    void testAccountAllArgsConstructor() {
        // Arrange
        String accountId = "ACC003";
        BigDecimal balance = new BigDecimal("500.00");
        Instant now = Instant.now();

        // Act
        Account account = new Account(accountId, balance, now);

        // Assert
        assertEquals(accountId, account.getAccountId());
        assertEquals(balance, account.getBalance());
        assertEquals(now, account.getUpdatedAt());
    }

    @Test
    void testAccountBalanceOperations() {
        // Arrange
        Account account = Account.builder()
                .accountId("ACC001")
                .balance(new BigDecimal("100.00"))
                .updatedAt(Instant.now())
                .build();

        BigDecimal creditAmount = new BigDecimal("50.00");
        BigDecimal expectedBalance = new BigDecimal("150.00");

        // Act
        BigDecimal newBalance = account.getBalance().add(creditAmount);
        account.setBalance(newBalance);

        // Assert
        assertEquals(expectedBalance, account.getBalance());
    }
}
