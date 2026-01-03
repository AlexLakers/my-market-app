package com.alex.market.search;

import jakarta.persistence.criteria.Predicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ItemSpecification {
    public static Specification<Item> getSpecByTitleOrDescription(String search) {
        return (root, query, builder) -> {
            if (search == null || search.trim().isEmpty()) {
                return builder.conjunction();
            }
            String likeFormat = "%" + search.toLowerCase() + "%";

            Predicate predicateTitleContainingIgCase = builder
                    .like(builder.lower(root.get(Item.Fields.title)),
                            likeFormat);

            Predicate predicateDescContainingIgCase = builder
                    .like(builder.lower(root.get(Item.Fields.description)),
                            likeFormat);

            return builder.or(predicateTitleContainingIgCase, predicateDescContainingIgCase);
        };
    }
}

