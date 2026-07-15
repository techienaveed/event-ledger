package com.sample.account;

import com.sample.account.dto.AccountResponse;
import com.sample.account.dto.BalanceResponse;
import com.sample.account.dto.TransactionRequest;
import com.sample.account.entity.Account;
import com.sample.account.repository.AccountRepository;
import com.sample.account.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AccountServiceApplicationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private String accountId;
    private String eventId;
    private BigDecimal amount;
    private Instant now;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        transactionRepository.deleteAll();

        accountId = "INT_ACC001";
        eventId = "INT_EVT001";
        amount = new BigDecimal("100.00");
        now = Instant.now();
    }

    @Test
    void testFullTransactionFlow() throws Exception {
        // Step 1: Apply credit transaction to new account
        TransactionRequest creditRequest = new TransactionRequest(
                "INT_EVT001",
                accountId,
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                now
        );

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creditRequest)))
                .andExpect(status().isCreated());

        // Step 2: Verify balance
        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.balance").value(100.0));

        // Step 3: Apply debit transaction
        TransactionRequest debitRequest = new TransactionRequest(
                "INT_EVT002",
                accountId,
                "DEBIT",
                new BigDecimal("30.00"),
                "USD",
                now
        );

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(debitRequest)))
                .andExpect(status().isCreated());

        // Step 4: Verify updated balance
        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(70.0));

        // Step 5: Get full account details
        mockMvc.perform(get("/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId").value(accountId))
                .andExpect(jsonPath("$.balance").value(70.0))
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions.length()").value(2));
    }

    @Test
    void testIdempotentTransactions() throws Exception {
        // Arrange
        TransactionRequest request = new TransactionRequest(
                "INT_EVT003",
                accountId,
                "CREDIT",
                new BigDecimal("50.00"),
                "USD",
                now
        );

        // Act - First transaction
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Act - Duplicate transaction (same event ID)
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Assert - Balance should be 50, not 100
        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.0));
    }

    @Test
    void testMultipleAccountsIndependence() throws Exception {
        // Arrange
        String accountId2 = "INT_ACC002";

        TransactionRequest request1 = new TransactionRequest(
                "INT_EVT004",
                accountId,
                "CREDIT",
                new BigDecimal("100.00"),
                "USD",
                now
        );

        TransactionRequest request2 = new TransactionRequest(
                "INT_EVT005",
                accountId2,
                "CREDIT",
                new BigDecimal("50.00"),
                "USD",
                now
        );

        // Act - Credit to first account
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Act - Credit to second account
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // Assert - Verify both accounts have independent balances
        mockMvc.perform(get("/accounts/{accountId}/balance", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(100.0));

        mockMvc.perform(get("/accounts/{accountId}/balance", accountId2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(50.0));
    }

    @Test
    void testTransactionOrdering() throws Exception {
        // Arrange
        Instant time1 = Instant.parse("2026-07-15T00:00:00Z");
        Instant time2 = Instant.parse("2026-07-15T01:00:00Z");
        Instant time3 = Instant.parse("2026-07-15T02:00:00Z");

        TransactionRequest request1 = new TransactionRequest(
                "INT_EVT006",
                accountId,
                "CREDIT",
                new BigDecimal("10.00"),
                "USD",
                time1
        );

        TransactionRequest request2 = new TransactionRequest(
                "INT_EVT007",
                accountId,
                "CREDIT",
                new BigDecimal("20.00"),
                "USD",
                time3
        );

        TransactionRequest request3 = new TransactionRequest(
                "INT_EVT008",
                accountId,
                "CREDIT",
                new BigDecimal("30.00"),
                "USD",
                time2
        );

        // Act - Apply transactions in non-chronological order
        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isCreated());

        // Assert - Verify transactions are returned in chronological order
        mockMvc.perform(get("/accounts/{accountId}", accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions[0].eventId").value("INT_EVT006"))
                .andExpect(jsonPath("$.transactions[1].eventId").value("INT_EVT008"))
                .andExpect(jsonPath("$.transactions[2].eventId").value("INT_EVT007"));
    }
}
