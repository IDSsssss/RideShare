package com.example.rideshare.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
@Schema(description = "User creation/update request")
public class UserRequestDto {

    @Schema(description = "Full name of the user", example = "Иван Петров", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Zа-яА-Я\\s-]+$", message = "Name can only contain letters, spaces and hyphens")
    private String name;

    @Schema(description = "Email address", example = "ivan.petrov@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$", message = "Invalid email format")
    private String email;

    @Schema(description = "Phone number", example = "+79001234567", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?\\d{10,15}$", message = "Phone must be 10-15 digits, optionally starting with +")
    private String phone;

    @Schema(description = "Login password: set on create or when changing (min 8 characters when not empty)",
            example = "secretpass", maxLength = 128)
    @Size(max = 128, message = "Password must be at most 128 characters")
    private String password;

    @Schema(description = "User rating", example = "4.5", minimum = "0", maximum = "5")
    @Min(value = 0, message = "Rating must be at least 0")
    @Max(value = 5, message = "Rating must be at most 5")
    private Double rating;
}