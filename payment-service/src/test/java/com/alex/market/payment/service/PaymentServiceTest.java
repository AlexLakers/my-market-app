package com.alex.market.payment.service;

import com.alex.market.payment.api.dto.PaymentRequest;
import com.alex.market.payment.api.dto.PaymentResponse;
import com.alex.market.payment.api.dto.TransactionStatus;
import com.alex.market.payment.mapper.AccountMapper;
import com.alex.market.payment.mapper.TransactionMapper;
import com.alex.market.payment.model.Transaction;
import com.alex.market.payment.model.TransactionType;
import com.alex.market.payment.repository.AccountRepository;
import com.alex.market.payment.repository.TransactionRepository;
import com.alex.market.payment.service.impl.AccountServiceImpl;
import com.alex.market.payment.service.impl.PaymentServiceImpl;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@SpringJUnitConfig
class PaymentServiceTest {

    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TransactionMapper transactionMapper;
    @Autowired
    private PaymentService paymentService;

    private Long transactionId = 1L;
    private Long orderId = 1L;
    private Long accountId = 1L;
    private Long userId = 1L;
    private Long amount = 300L;


    @BeforeEach
    void setUp() {
        Mockito.reset(transactionRepository, accountRepository, transactionMapper);
    }

    @Test
    void processPaymentInTransaction_shouldReturnResponseWithExistTransaction_Failed() {
        PaymentRequest request = new PaymentRequest(accountId, userId, orderId, amount);
        PaymentResponse response = new PaymentResponse(accountId, orderId, transactionId, TransactionStatus.SUCCESS, 300L);
        Transaction transaction = Transaction.builder().id(transactionId).accountId(accountId).orderId(orderId).status(com.alex.market.payment.model.TransactionStatus.SUCCESS).type(TransactionType.PAYMENT).amount(amount).build();
        Mockito.when(transactionRepository.findByOrderId(orderId)).thenReturn(Mono.just(transaction));
        Mockito.when(transactionMapper.toPaymentResponse(transaction)).thenReturn(response);

        StepVerifier.create(paymentService.processPaymentInTransaction(request))
                .expectNext(response)
                .verifyComplete();

        verify(transactionRepository, Mockito.times(1)).findByOrderId(orderId);
        verify(transactionMapper, Mockito.times(1)).toPaymentResponse(transaction);
        verify(accountRepository, Mockito.never()).decrementBalanceAtomic(accountId, amount);

    }

    @Test
    void processPaymentInTransaction_shouldProcessAndReturnResponseWithNewSuccessTransaction_Success() {
        PaymentRequest request = new PaymentRequest(accountId, userId, orderId, amount);
        PaymentResponse response = new PaymentResponse(accountId, orderId, transactionId, TransactionStatus.SUCCESS, 300L);
        Transaction transaction = Transaction.builder().id(transactionId).accountId(accountId).orderId(orderId).status(com.alex.market.payment.model.TransactionStatus.SUCCESS).type(TransactionType.PAYMENT).amount(amount).build();

        Mockito.when(transactionRepository.findByOrderId(orderId)).thenReturn(Mono.empty());
        Mockito.when(transactionMapper.toPaymentResponse(transaction)).thenReturn(response);
        Mockito.when(accountRepository.decrementBalanceAtomic(accountId, amount)).thenReturn(Mono.just(1L));
        Mockito.when(transactionRepository.save(Mockito.any(Transaction.class))).thenReturn(Mono.just(transaction));

        StepVerifier.create(paymentService.processPaymentInTransaction(request))
                .expectNext(response)
                .verifyComplete();
    }

    @ParameterizedTest
    @CsvSource({
            "-1, 300, Only account with id=1 is supported",
            "1, -300 , Amount must be positive and account with id: 1"
    })
    void processPaymentInTransaction_shouldNotProcessAndReturnResponseWithFailedTransactionWithMessage_wheAccountIdIncorrectOrAmount(Long accountId, Long amount, String failureReason) {
        PaymentRequest request = new PaymentRequest(accountId, userId, orderId, amount);
        PaymentResponse response = new PaymentResponse(accountId, orderId, transactionId, TransactionStatus.FAILED, 300L);
        response.setFailureReason(failureReason);
        Transaction transaction = Transaction.builder().id(transactionId).accountId(accountId).orderId(orderId).status(com.alex.market.payment.model.TransactionStatus.FAILED).type(TransactionType.PAYMENT).amount(amount).failureReason(failureReason).build();

        Mockito.when(transactionRepository.findByOrderId(orderId)).thenReturn(Mono.empty());
        Mockito.when(transactionMapper.toPaymentResponse(transaction)).thenReturn(response);
        Mockito.when(transactionRepository.save(Mockito.any(Transaction.class))).thenReturn(Mono.just(transaction));


        StepVerifier.create(paymentService.processPaymentInTransaction(request))
                .expectNext(response)
                .verifyComplete();

    }

    @TestConfiguration
    static class PaymentServiceTestContextConfiguration {
        @Bean
        public TransactionMapper transactionMapper() {
            return Mockito.mock(TransactionMapper.class);
        }

        @Bean
        public AccountRepository accountRepository() {
            return Mockito.mock(AccountRepository.class);
        }

        @Bean
        public TransactionRepository transactionRepository() {
            return Mockito.mock(TransactionRepository.class);
        }

        @Bean
        public PaymentService paymentService(AccountRepository accountRepository,
                                             TransactionRepository transactionRepository,
                                             TransactionMapper transactionMapper) {
            return new PaymentServiceImpl(transactionRepository, accountRepository, transactionMapper);
        }
    }
}