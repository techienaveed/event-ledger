package com.sample.account.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        String eventId,
        String type,
        BigDecimal amount,
        String currency,
        Instant eventTimestamp
) {}