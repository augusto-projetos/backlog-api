package com.augustoprojetos.backlogapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateProfileDTO(
    @NotBlank(message = "O @ não pode estar vazio")
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "O @ só pode conter letras, números, sublinhados (_) ou pontos (.)")
    String socialUsername,
    
    boolean isPublic
) {}