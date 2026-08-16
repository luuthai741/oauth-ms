package com.example.authservice.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Jwk {
    @JsonProperty("kty")
    private String keyType;
    
    @JsonProperty("use")
    private String use;
    
    @JsonProperty("kid")
    private String keyId;
    
    @JsonProperty("n")
    private String modulus;
    
    @JsonProperty("e")
    private String publicExponent;
    
    @JsonProperty("alg")
    private String algorithm;
}

