package com.alex.market.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldNameConstants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Entity
@Getter
@Setter
@ToString(exclude = "orders")
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
    @ManyToMany(mappedBy = "items")
    private List<Order> orders = new ArrayList<>();

}

