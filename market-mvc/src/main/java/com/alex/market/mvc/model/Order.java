package com.alex.market.mvc.model;


import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
@Builder
@FieldNameConstants
@Table(name = "orders")
public class Order {

    @Id
    private Long id;

    @Column("total_sum")
    private Long totalSum;

    @Column("status")
    private OrderStatus status;

    @Column("user_id")
    private Long userId;
}
