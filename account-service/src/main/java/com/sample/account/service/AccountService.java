package com.sample.account.service;

import com.sample.account.dto.*;

import com.sample.account.exception.AccountNotFoundException;

public interface AccountService {

    void applyTransaction(TransactionRequest request);

    BalanceResponse getBalance(String accountId) throws AccountNotFoundException;

    AccountResponse getAccount(String accountId) throws AccountNotFoundException;

}
