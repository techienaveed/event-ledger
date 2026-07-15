package com.sample.account.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountNotFoundExceptionTest {

    @Test
    void testAccountNotFoundExceptionWithMessage() {
        // Arrange
        String accountId = "ACC001";
        String expectedMessage = "Account not found: ACC001";

        // Act
        AccountNotFoundException exception = new AccountNotFoundException(accountId);

        // Assert
        assertNotNull(exception);
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void testAccountNotFoundExceptionIsRuntimeException() {
        // Arrange
        String accountId = "ACC001";

        // Act
        AccountNotFoundException exception = new AccountNotFoundException(accountId);

        // Assert
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void testAccountNotFoundExceptionThrowable() {
        // Arrange
        String accountId = "ACC002";

        // Act & Assert
        assertThrows(AccountNotFoundException.class, () -> {
            throw new AccountNotFoundException(accountId);
        });
    }

    @Test
    void testAccountNotFoundExceptionMessage() {
        // Arrange
        String accountId = "ACC003";

        // Act
        AccountNotFoundException exception = new AccountNotFoundException(accountId);

        // Assert
        assertTrue(exception.getMessage().contains(accountId));
    }
}
