package com.alex.market.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
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
@Table(name = "items")
@FieldNameConstants
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "items_seq")
    @SequenceGenerator(name = "items_seq", sequenceName = "items_sequence", allocationSize = 1)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String title;

    @Column(nullable = true, length = 512)
    private String description;

    @Column(nullable = false, length = 128)
    private String imgPath;

    @Column(nullable = false)
    private Long price;

    @Builder.Default
    @OneToMany(mappedBy = "item")
    @BatchSize(size = 50)
    private List<OrderItem> orderItems = new ArrayList<>();

}

