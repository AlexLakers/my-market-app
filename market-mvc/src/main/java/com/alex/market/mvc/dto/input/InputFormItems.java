package com.alex.market.mvc.dto.input;

import com.alex.market.mvc.model.CartAction;
import com.alex.market.mvc.search.SortColumn;
import com.alex.market.mvc.validation.ValidMessages;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InputFormItems(
            @NotNull(message = ValidMessages.ID_REQUIRED)
            @Positive(message = ValidMessages.ID_POSITIVE)
            Long id,

            @NotNull(message = ValidMessages.ACTION_REQUIRED)
            CartAction action,

            String search,

            SortColumn sort,

            @Min(value = 1, message = ValidMessages.PAGE_MIN)
            Integer pageNumber,

            @Min(value = 1, message = ValidMessages.SIZE_MIN)
            @Max(value = 100, message = ValidMessages.SIZE_MAX)
            Integer pageSize
    ) {
        public InputFormItems {
            if (sort == null) sort = SortColumn.NO;
            if (pageNumber == null) pageNumber = 1;
            if (pageSize == null) pageSize = 10;
            if (search == null) search = "";
        }
    }
