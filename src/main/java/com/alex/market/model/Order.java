package com.alex.market.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Setter
@ToString(exclude = "orderItems")
@EqualsAndHashCode(of = "id")
@Builder
@FieldNameConstants
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "orders_sequence", allocationSize = 1)
    private Long id;

    @Column(name = "total_sum", nullable = false)
    private Long totalSum;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    private List<OrderItem> orderItems = new ArrayList<>();

    public void addItem(Item item, Integer count) {
        OrderItem orderItem = OrderItem.builder()
                .item(item)
                .count(count)
                .historyPrice(item.getPrice())
                .order(this)
                .build();
        this.orderItems.add(orderItem);

    }
}
