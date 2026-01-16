package com.alex.market.payment.repository;

import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;


public interface  QueryAccountRepository {
    Mono<Long> decrementBalanceAtomic(Long id, Long amount);
}
