package com.alex.market.payment.service.impl;

import com.alex.market.payment.api.dto.PaymentRequest;
import com.alex.market.payment.api.dto.PaymentResponse;
import com.alex.market.payment.mapper.TransactionMapper;
import com.alex.market.payment.model.Transaction;
import com.alex.market.payment.model.TransactionStatus;
import com.alex.market.payment.model.TransactionType;
import com.alex.market.payment.repository.AccountRepository;
import com.alex.market.payment.repository.TransactionRepository;
import com.alex.market.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final static Long GENERAL_ACCOUNT_ID = 1L;
    private final static Long SYSTEM_FAILED_ACCOUNT_ID = 0L;

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionMapper transactionMapper;

    @Override
    public Mono<PaymentResponse> processPaymentInTransaction(PaymentRequest paymentRequest) {
        log.info("Processing payment for order with id: {}", paymentRequest.getOrderId());

        return transactionRepository.findByOrderId(paymentRequest.getOrderId())
                .flatMap(existingTransaction -> {
                    log.info("Transaction for order with id: {} already exists", paymentRequest.getOrderId());
                    return Mono.just(transactionMapper.toPaymentResponse(existingTransaction));
                })
                .switchIfEmpty(Mono.defer(() -> {

                    if (paymentRequest.getAccountId() != null &&
                        !GENERAL_ACCOUNT_ID.equals(paymentRequest.getAccountId())) {
                        log.warn("Invalid accountId: {} for order with id: {}, expected: {}",
                                paymentRequest.getAccountId(), paymentRequest.getOrderId(), GENERAL_ACCOUNT_ID);

                        return createFailedTransaction(
                                paymentRequest,
                                String.format("Only account with id=%d is supported", GENERAL_ACCOUNT_ID),
                                TransactionType.PAYMENT
                        ).map(transactionMapper::toPaymentResponse);
                    }

                    if (paymentRequest.getAmount() == null || paymentRequest.getAmount() <= 0) {
                        log.warn("Invalid amount: {} for order {}",
                                paymentRequest.getAmount(), paymentRequest.getOrderId());
                        return createFailedTransaction(
                                paymentRequest,
                                String.format("Amount must be positive and account with id: %d", GENERAL_ACCOUNT_ID),
                                TransactionType.PAYMENT
                        ).map(transactionMapper::toPaymentResponse);
                    }

                    return accountRepository.decrementBalanceAtomic(
                                    GENERAL_ACCOUNT_ID,
                                    paymentRequest.getAmount())
                            .flatMap(rowsUpdated -> {
                                if (rowsUpdated == 1) {
                                    log.debug("Successfully withdrew {} from account {}",
                                            paymentRequest.getAmount(), GENERAL_ACCOUNT_ID);
                                    return createSuccessTransaction(paymentRequest, TransactionType.PAYMENT)
                                            .map(transactionMapper::toPaymentResponse);
                                } else {
                                    log.warn("Insufficient funds in account with id: {}",
                                            GENERAL_ACCOUNT_ID);
                                    return createFailedTransaction(
                                            paymentRequest,
                                            String.format("Insufficient funds in account with id: %d",
                                                    GENERAL_ACCOUNT_ID),
                                            TransactionType.PAYMENT
                                    ).map(transactionMapper::toPaymentResponse);
                                }
                            });
                }))
                .doOnSuccess(response ->
                        log.info("Payment processed: {} for order with id: {}",
                                response.getStatus(), paymentRequest.getOrderId())
                )
                .doOnError(error ->
                        log.error("Payment processing failed: {} for order with id: {}",
                                error.getMessage(), paymentRequest.getOrderId())
                );
    }


    private Mono<Transaction> createSuccessTransaction(PaymentRequest paymentRequest, TransactionType type) {

        Transaction successTransaction = Transaction.builder()
                .accountId(GENERAL_ACCOUNT_ID)
                .orderId(paymentRequest.getOrderId())
                .amount(paymentRequest.getAmount())
                .status(TransactionStatus.SUCCESS)
                .type(type)
                .build();
        return transactionRepository.save(successTransaction);
    }

    private Mono<Transaction> createFailedTransaction(PaymentRequest paymentRequest, String failureReason, TransactionType type) {
        Transaction failedTransaction = Transaction.builder()
                .accountId(SYSTEM_FAILED_ACCOUNT_ID)
                .orderId(paymentRequest.getOrderId())
                .amount(paymentRequest.getAmount())
                .status(TransactionStatus.FAILED)
                .type(type)
                .failureReason(failureReason)
                .build();
        return transactionRepository.save(failedTransaction);
    }

}
