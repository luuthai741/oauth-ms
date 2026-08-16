package com.example.authservice.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExternalLoginRequest {

    @NotBlank
    private String provider;

    @NotBlank
    private String accessToken;
}

