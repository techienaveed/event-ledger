package com.sample.account.repository;



import com.sample.account.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {

    Optional<TransactionEntity> findByEventId(String eventId);

    List<TransactionEntity> findByAccountIdOrderByEventTimestampAsc(String accountId);
}