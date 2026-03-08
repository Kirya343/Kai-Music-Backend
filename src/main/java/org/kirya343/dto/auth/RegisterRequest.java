package org.kirya343.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank
    @Pattern(
        regexp = "^[A-Za-z0-9_]{3,16}$",
        message = "Ник должен содержать только латинские буквы, цифры и знак подчеркивания, длина 3–16 символов"
    )
    String name,

    @NotBlank
    @Email
    String email,

    @Size(min = 8)
    String password
) {
}