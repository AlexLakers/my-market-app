package com.alex.market.mvc.cache;

import java.io.Serializable;

public record ItemCache(
            Long id,
            String title,
            String description,
            Long price,
            String imgPath
) {
}
