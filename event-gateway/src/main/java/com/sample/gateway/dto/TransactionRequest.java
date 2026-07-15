package com.sample.gateway.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequest(

        String eventId,

        String accountId,

        String type,

        BigDecimal amount,

        String currency,

        Instant eventTimestamp

){}