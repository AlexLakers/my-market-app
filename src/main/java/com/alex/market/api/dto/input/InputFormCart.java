package com.alex.market.api.dto.input;

import com.alex.market.model.CartAction;
import com.alex.market.validator.ValidMessages;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InputFormCart(@NotNull(message = ValidMessages.ID_REQUIRED)
                            @Positive(message = ValidMessages.ID_POSITIVE)
                            Long id,

                            @NotNull(message = ValidMessages.ACTION_REQUIRED)
                            CartAction action) {
}
