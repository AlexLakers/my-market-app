package com.alex.market.payment.service.impl;

import com.alex.market.payment.api.dto.AccountResponse;
import com.alex.market.payment.exception.AccountNotFoundException;
import com.alex.market.payment.mapper.AccountMapper;
import com.alex.market.payment.repository.AccountRepository;
import com.alex.market.payment.service.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    @Override
    public Mono<AccountResponse> getAccountById(Long id) {
        log.info("Get account with balance by id: {}", id);

        return accountRepository.findById(id)
                .switchIfEmpty(Mono.error(new AccountNotFoundException(id)))
                .map(acc -> {
                    log.debug("Found account with balance: {} by id:{}", acc.getBalance(), acc.getId());
                    return accountMapper.toAccountResponse(acc);
                });
    }
}
