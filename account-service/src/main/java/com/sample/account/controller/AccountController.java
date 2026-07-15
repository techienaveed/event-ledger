package com.sample.account.controller;

import com.sample.account.dto.AccountResponse;
import com.sample.account.dto.BalanceResponse;
import com.sample.account.dto.TransactionRequest;
import com.sample.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.sample.account.exception.AccountNotFoundException;

@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping("/{accountId}/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public void applyTransaction(
            @PathVariable String accountId,
            @Valid @RequestBody TransactionRequest request) {

        service.applyTransaction(request);
    }

    @GetMapping("/{accountId}/balance")
    public BalanceResponse balance(@PathVariable String accountId) throws AccountNotFoundException {
        return service.getBalance(accountId);
    }

    @GetMapping("/{accountId}")
    public AccountResponse account(@PathVariable String accountId) throws AccountNotFoundException{
        return service.getAccount(accountId);
    }

    @ExceptionHandler(AccountNotFoundException.class)

   @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public void handleAccountNotFoundException(AccountNotFoundException ex) {}
}