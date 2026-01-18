package com.alex.market.payment.service;


import com.alex.market.payment.api.dto.AccountResponse;
import com.alex.market.payment.model.Account;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

public interface AccountService {
    Mono<AccountResponse> getAccountById(Long id);
}
