package com.alex.market.api.dto.input;

import com.alex.market.validator.ValidImage;
import com.alex.market.validator.ValidMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.multipart.MultipartFile;

public record ItemCreateDto(@NotBlank(message = ValidMessages.TITLE_REQUIRED)
                            String title,

                            @NotBlank(message = ValidMessages.DESC_REQUIRED)
                            String description,

                            @ValidImage
                            MultipartFile image,

                            @NotNull(message = ValidMessages.PRICE_REQUIRED)
                            @Positive(message = ValidMessages.PRICE_POSITIVE)
                            Long price) {
}
