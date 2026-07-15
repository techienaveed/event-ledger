package com.sample.account.service;

import com.sample.account.dto.AccountResponse;
import com.sample.account.dto.BalanceResponse;
import com.sample.account.dto.TransactionRequest;
import com.sample.account.dto.TransactionResponse;
import com.sample.account.entity.Account;
import com.sample.account.entity.TransactionEntity;
import com.sample.account.repository.AccountRepository;
import com.sample.account.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sample.account.exception.AccountNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public void applyTransaction(TransactionRequest request) {

        if (transactionRepository.findByEventId(request.eventId()).isPresent()) {
            return;
        }

        Account account = accountRepository.findById(request.accountId())
                .orElse(Account.builder()
                        .accountId(request.accountId())
                        .balance(BigDecimal.ZERO)
                        .updatedAt(Instant.now())
                        .build());

        BigDecimal newBalance =
                request.type().equalsIgnoreCase("CREDIT")
                        ? account.getBalance().add(request.amount())
                        : account.getBalance().subtract(request.amount());

        account.setBalance(newBalance);
        account.setUpdatedAt(Instant.now());

        accountRepository.save(account);

        TransactionEntity entity = TransactionEntity.builder()
                .eventId(request.eventId())
                .accountId(request.accountId())
                .type(request.type())
                .amount(request.amount())
                .currency(request.currency())
                .eventTimestamp(request.eventTimestamp())
                .build();

        transactionRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountId)  {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        return new BalanceResponse(
                account.getAccountId(),
                account.getBalance());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountId)  {

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        List<TransactionResponse> transactions =
                transactionRepository
                        .findByAccountIdOrderByEventTimestampAsc(accountId)
                        .stream()
                        .map(t -> new TransactionResponse(
                                t.getEventId(),
                                t.getType(),
                                t.getAmount(),
                                t.getCurrency(),
                                t.getEventTimestamp()))
                        .toList();

        return new AccountResponse(
                accountId,
                account.getBalance(),
                transactions);
    }
}