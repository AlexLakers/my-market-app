package com.alex.market.payment.repository;

import com.alex.market.payment.model.Transaction;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface TransactionRepository extends ReactiveCrudRepository<Transaction, Long> {
    Mono<Transaction> findByOrderId(Long orderId);
}
