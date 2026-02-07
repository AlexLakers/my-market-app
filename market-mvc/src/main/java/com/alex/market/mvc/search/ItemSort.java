package com.alex.market.mvc.search;

import com.alex.market.mvc.model.Item;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemSort {

    public static Sort getOrderByPriceOrTitle(SortColumn sortColumn) {
        return switch (sortColumn) {
            case NO -> Sort.by(Item.Fields.id);
            case PRICE -> Sort.by(Item.Fields.price);
            case ALPHA -> Sort.by(Item.Fields.title);
        };
    }
}
