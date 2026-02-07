package com.alex.market.mvc.security.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserDto(Long id,
                      String firstname,
                      String lastname,
                      String username,
                      String role,
                      @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
                      LocalDate birthday
                      ) {
}
