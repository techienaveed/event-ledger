package com.sample.gateway.servicegateway;


import com.sample.gateway.client.AccountClient;
import com.sample.gateway.dto.TransactionRequest;
import com.sample.gateway.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeignAccountServiceGateway implements AccountServiceGateway {

    private final AccountClient accountClient;

    @Override
    @Retry(name = "accountService")
    @CircuitBreaker(
            name = "accountService",
            fallbackMethod = "accountFallback")
    public void applyTransaction(TransactionRequest request) {

        accountClient.applyTransaction(
                request.accountId(),
                request);

    }

    public void accountFallback(
            TransactionRequest request,
            Throwable ex) throws ServiceUnavailableException {

        throw new ServiceUnavailableException(
                "Account Service is unavailable");
    }
}