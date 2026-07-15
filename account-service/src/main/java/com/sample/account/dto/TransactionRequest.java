package com.sample.account.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequest(

        @NotBlank
        String eventId,

        @NotBlank
        String accountId,

        @NotBlank
        String type,

        @Positive
        BigDecimal amount,

        @NotBlank
        String currency,

        @NotNull
        //@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
        Instant eventTimestamp
) {}