package com.sample.gateway.servicegateway;


import com.sample.gateway.dto.TransactionRequest;

public interface AccountServiceGateway {

    void applyTransaction(TransactionRequest request);

}
