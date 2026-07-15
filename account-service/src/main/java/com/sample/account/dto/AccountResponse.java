package com.sample.account.dto;

import java.math.BigDecimal;
import java.util.List;

public record AccountResponse(

        String accountId,

        BigDecimal balance,

        List<TransactionResponse> transactions
) {
}