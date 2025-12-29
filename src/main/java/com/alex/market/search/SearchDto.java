package com.alex.market.search;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.validator.constraints.Length;

import java.util.Map;

public record SearchDto(
                        String search,
                        @NotNull(message = "Sort column should be not null value")
                        SortColumn sortColumn,

                        @PositiveOrZero(message = "Page number should be positive value or zero")
                        @NotNull(message = "Page number should be not null value")
                        int pageNumber,

                        @NotNull(message = "Page size should be not null value")
                        @Positive(message = "Page size should be positive value")
                        int pageSize,

                        Map <Long, Integer> cartItemsCount) {
}