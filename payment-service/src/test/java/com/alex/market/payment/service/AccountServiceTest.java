package com.alex.market.payment.service;

import com.alex.market.payment.api.dto.AccountResponse;
import com.alex.market.payment.exception.AccountNotFoundException;
import com.alex.market.payment.mapper.AccountMapper;
import com.alex.market.payment.model.Account;
import com.alex.market.payment.repository.AccountRepository;
import com.alex.market.payment.service.impl.AccountServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringJUnitConfig
class AccountServiceTest {

    private final Long VALID_ID = 1L;
    private final Long INVALID_ID = -1L;

    @Autowired
    private AccountMapper accountMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Test
    void getAccountById_shouldReturnAccountWithIdSuccess() {
        Account foundedAccount = Account.builder().id(VALID_ID).build();
        AccountResponse resp = new AccountResponse();
        resp.accountId(VALID_ID);
        when(accountRepository.findById(VALID_ID)).thenReturn(Mono.just(foundedAccount));
        when(accountMapper.toAccountResponse(foundedAccount)).thenReturn(resp);

        StepVerifier.create(accountService.getAccountById(VALID_ID))
                .expectNext(resp)
                .verifyComplete();
    }

    @Test
    void getAccountById_shouldThrowAccountNotFoundExceptionFailed() {
        when(accountRepository.findById(INVALID_ID)).thenReturn(Mono.empty());

        StepVerifier.create(accountService.getAccountById(INVALID_ID))
                .expectError(AccountNotFoundException.class)
                .verify();
        Mockito.verify(accountRepository, Mockito.times(1)).findById(INVALID_ID);
        Mockito.verify(accountMapper, Mockito.times(0)).toAccountResponse(Mockito.any(Account.class));
    }

    @TestConfiguration
    static class AccountServiceTestContextConfiguration {
        @Bean
        public AccountMapper accountMapper() {
            return Mockito.mock(AccountMapper.class);
        }

        @Bean
        public AccountRepository accountRepository() {
            return Mockito.mock(AccountRepository.class);
        }

        @Bean
        public AccountService accountService(AccountRepository accountRepository, AccountMapper accountMapper) {
            return new AccountServiceImpl(accountRepository, accountMapper);
        }

    }
}