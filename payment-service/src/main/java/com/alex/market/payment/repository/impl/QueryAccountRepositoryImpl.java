package com.alex.market.payment.repository.impl;

import com.alex.market.payment.repository.AccountRepository;
import com.alex.market.payment.repository.QueryAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class QueryAccountRepositoryImpl implements QueryAccountRepository {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Long> decrementBalanceAtomic(Long accountId, Long amount) {
        return databaseClient.sql(""" 
                        UPDATE accounts
                        SET balance = balance - :amount
                        WHERE id = :id AND balance >= :amount
                        """)
                .bind("id", accountId)
                .bind("amount", amount)
                .fetch()
                .rowsUpdated();
    }
}
