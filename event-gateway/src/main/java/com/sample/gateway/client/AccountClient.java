package com.sample.gateway.client;

import com.sample.gateway.dto.TransactionRequest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name="account-service",
        url="${account.service.url}"
)
public interface AccountClient {


    @PostMapping("/accounts/{accountId}/transactions")
    void applyTransaction(
            @PathVariable String accountId,
            @RequestBody TransactionRequest request);

}