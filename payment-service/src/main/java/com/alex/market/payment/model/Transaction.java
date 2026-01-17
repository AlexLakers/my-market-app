package com.alex.market.payment.model;

import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@Table("transactions")
@FieldNameConstants
public class Transaction {

    @Id
    private Long id;

    @Column("account_id")
    private Long accountId;

    @Column("order_id")
    private Long orderId;

    @Column("amount")
    private Long amount;

    @Column("status")
    private TransactionStatus status;

    @Column("type")
    private TransactionType type;

    @Column("failure_reason")
    private String failureReason;

    @Column("created_at")
    private LocalDateTime createdAt;

    public static Transaction success(Long accountId,
                                      Long orderId,
                                      Long amount,
                                      TransactionType type) {
        return Transaction.builder()
                .status(TransactionStatus.SUCCESS)
                .accountId(accountId)
                .orderId(orderId)
                .amount(amount)
                .type(type)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Transaction failed(Long accountId,
                                     Long orderId,
                                     Long amount,
                                     TransactionType type) {
        return Transaction.builder()
                .status(TransactionStatus.FAILED)
                .accountId(accountId)
                .orderId(orderId)
                .amount(amount)
                .type(type)
                .createdAt(LocalDateTime.now())
                .build();
    }


}

