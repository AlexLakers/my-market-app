package com.alex.market.mvc.dto.input;

import com.alex.market.mvc.validation.ValidMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemCreateDto(@NotBlank(message = ValidMessages.TITLE_REQUIRED)
                            String title,

                            @NotBlank(message = ValidMessages.DESC_REQUIRED)
                            String description,

                            @NotNull(message = ValidMessages.PRICE_REQUIRED)
                            @Positive(message = ValidMessages.PRICE_POSITIVE)
                            Long price) {
}
