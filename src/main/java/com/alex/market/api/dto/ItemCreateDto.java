package com.alex.market.api.dto;

import com.alex.market.validator.ValidImage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.multipart.MultipartFile;

public record ItemCreateDto(@NotBlank String title,
                            @NotBlank @Length String description,
                            @ValidImage MultipartFile image,
                            @Positive @NotNull Long price) {
}
