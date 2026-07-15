package com.sample.account.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sample.account.dto.AccountResponse;
import com.sample.account.dto.BalanceResponse;
import com.sample.account.dto.TransactionRequest;
import com.sample.account.dto.TransactionResponse;
import com.sample.account.exception.AccountNotFoundException;
import com.sample.account.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private AccountService accountService;

    private String accountId;
    private String eventId;
    private BigDecimal amount;
    private Instant now;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new AccountController(accountService)).build();

        accountId = "ACC001";
        eventId = "EVT001";
        amount = new BigDecimal("100.00");
        now = Instant.now();
    }

    @Nested
    class ApplyTransactionTests {

        @Test
        void shouldApplyTransactionSuccessfully() throws Exception {
            TransactionRequest request = new TransactionRequest(
                    eventId,
                    accountId,
                    "CREDIT",
                    amount,
                    "USD",
                    now
            );

            doNothing().when(accountService).applyTransaction(any(TransactionRequest.class));

            mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());

            verify(accountService).applyTransaction(any(TransactionRequest.class));
        }

        @Test
        void shouldRejectInvalidTransaction() throws Exception {
            String invalidRequest = "{}";

            mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());

            verify(accountService, never()).applyTransaction(any(TransactionRequest.class));
        }

        @Test
        void shouldApplyMultipleTransactions() throws Exception {
            TransactionRequest request1 = new TransactionRequest(
                    "EVT001",
                    accountId,
                    "CREDIT",
                    new BigDecimal("100.00"),
                    "USD",
                    now
            );

            TransactionRequest request2 = new TransactionRequest(
                    "EVT002",
                    accountId,
                    "DEBIT",
                    new BigDecimal("50.00"),
                    "USD",
                    now
            );

            doNothing().when(accountService).applyTransaction(any(TransactionRequest.class));

            mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request1)))
                    .andExpect(status().isCreated());

            mockMvc.perform(post("/accounts/{accountId}/transactions", accountId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request2)))
                    .andExpect(status().isCreated());

            verify(accountService, times(2)).applyTransaction(any(TransactionRequest.class));
        }
    }

    @Nested
    class GetBalanceTests {

        @Test
        void shouldGetBalanceSuccessfully() throws Exception {
            BalanceResponse response = new BalanceResponse(accountId, amount);
            when(accountService.getBalance(accountId)).thenReturn(response);

            mockMvc.perform(get("/accounts/{accountId}/balance", accountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(accountId))
                    .andExpect(jsonPath("$.balance").value(100.00));

            verify(accountService).getBalance(accountId);
        }

        @Test
        void shouldThrowExceptionWhenAccountNotFound() throws Exception {
            when(accountService.getBalance(accountId))
                    .thenThrow(new AccountNotFoundException(accountId));

            mockMvc.perform(get("/accounts/{accountId}/balance", accountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());

            verify(accountService).getBalance(accountId);
        }

        @Test
        void shouldReturnZeroBalance() throws Exception {
            BalanceResponse response = new BalanceResponse(accountId, BigDecimal.ZERO);
            when(accountService.getBalance(accountId)).thenReturn(response);

            mockMvc.perform(get("/accounts/{accountId}/balance", accountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(0.0));

            verify(accountService).getBalance(accountId);
        }
    }

    @Nested
    class GetAccountTests {

        @Test
        void shouldGetAccountSuccessfully() throws Exception {
            TransactionResponse transaction = new TransactionResponse(
                    eventId,
                    "CREDIT",
                    amount,
                    "USD",
                    now
            );

            AccountResponse response = new AccountResponse(
                    accountId,
                    amount,
                    List.of(transaction)
            );

            when(accountService.getAccount(accountId)).thenReturn(response);

            mockMvc.perform(get("/accounts/{accountId}", accountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(accountId))
                    .andExpect(jsonPath("$.balance").value(100.00))
                    .andExpect(jsonPath("$.transactions").isArray())
                    .andExpect(jsonPath("$.transactions[0].eventId").value(eventId))
                    .andExpect(jsonPath("$.transactions[0].type").value("CREDIT"));

            verify(accountService).getAccount(accountId);
        }

        @Test
        void shouldThrowExceptionWhenAccountNotFound() throws Exception {
            when(accountService.getAccount(accountId))
                    .thenThrow(new AccountNotFoundException(accountId));

            mockMvc.perform(get("/accounts/{accountId}", accountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError());

            verify(accountService).getAccount(accountId);
        }

        @Test
        void shouldReturnAccountWithEmptyTransactions() throws Exception {
            AccountResponse response = new AccountResponse(
                    accountId,
                    BigDecimal.ZERO,
                    List.of()
            );

            when(accountService.getAccount(accountId)).thenReturn(response);

            mockMvc.perform(get("/accounts/{accountId}", accountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accountId").value(accountId))
                    .andExpect(jsonPath("$.balance").value(0.0))
                    .andExpect(jsonPath("$.transactions").isEmpty());

            verify(accountService).getAccount(accountId);
        }

        @Test
        void shouldReturnAccountWithMultipleTransactions() throws Exception {
            TransactionResponse transaction1 = new TransactionResponse(
                    "EVT001",
                    "CREDIT",
                    new BigDecimal("100.00"),
                    "USD",
                    now
            );

            TransactionResponse transaction2 = new TransactionResponse(
                    "EVT002",
                    "DEBIT",
                    new BigDecimal("50.00"),
                    "USD",
                    now
            );

            AccountResponse response = new AccountResponse(
                    accountId,
                    new BigDecimal("50.00"),
                    List.of(transaction1, transaction2)
            );

            when(accountService.getAccount(accountId)).thenReturn(response);

            mockMvc.perform(get("/accounts/{accountId}", accountId)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.balance").value(50.00))
                    .andExpect(jsonPath("$.transactions.length()").value(2))
                    .andExpect(jsonPath("$.transactions[0].type").value("CREDIT"))
                    .andExpect(jsonPath("$.transactions[1].type").value("DEBIT"));

            verify(accountService).getAccount(accountId);
        }
    }
}