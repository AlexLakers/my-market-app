package com.alex.market.mvc.security.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record UserRegDto(
        @NotBlank(message = "The firstname should be not blank")
        String firstname,

        @NotBlank(message = "The lastname should be not blank")
        String lastname,

        @NotBlank(message = "The username should be not blank")
        @Email(message = "Username should be like an email")
        String username,

        @NotBlank(message = "The pass should be not blank")
        @Pattern(regexp = "(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9]).{6,12}",
                message = "The password should contains digits and big letters")
        String password,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @Past
        LocalDate birthday
) {
}
