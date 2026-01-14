package com.alex.market.mvc.dto.input;

import com.alex.market.mvc.model.CartAction;
import com.alex.market.mvc.validation.ValidMessages;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InputFormCart(@NotNull(message = ValidMessages.ID_REQUIRED)
                            @Positive(message = ValidMessages.ID_POSITIVE)
                            Long id,

                            @NotNull(message = ValidMessages.ACTION_REQUIRED)
                            CartAction action) {
}
