package com.example.authservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalUserProfile {
    private String provider;
    private String providerId;
    private String email;
    private String firstName;
    private String lastName;
}

