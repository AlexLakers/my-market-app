package com.alex.market.payment.model;

import lombok.*;
import lombok.experimental.FieldNameConstants;
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
@Table("accounts")
@FieldNameConstants
public class Account {

    @Id
    private Long id;

    @Column("user_id")
    private Long userId;

    @Column("balance")
    @Builder.Default
    private Long balance= 0L;

    @Column("created_at")
    private LocalDateTime createdAt;

}
