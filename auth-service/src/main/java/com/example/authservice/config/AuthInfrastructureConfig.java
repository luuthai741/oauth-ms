package com.example.authservice.config;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Configuration
public class AuthInfrastructureConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RestClient restClient() {
        return RestClient.create();
    }

    @Bean
    public RSAKey rsaKey(
            @Value("${jwt.key.id:auth-service-key}") String keyId,
            @Value("${jwt.key.public-key-path:./keys/jwt-public.pem}") String publicKeyPath,
            @Value("${jwt.key.private-key-path:./keys/jwt-private.pem}") String privateKeyPath,
            @Value("${jwt.key.auto-generate-if-missing:true}") boolean autoGenerateIfMissing) {
        try {
            Path publicPath = Path.of(publicKeyPath);
            Path privatePath = Path.of(privateKeyPath);

            if (Files.exists(publicPath) && Files.exists(privatePath)) {
                return loadRsaKey(keyId, publicPath, privatePath);
            }

            if (!autoGenerateIfMissing) {
                throw new IllegalStateException("RSA key files not found and auto generation is disabled");
            }

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            savePem(publicPath, "PUBLIC KEY", keyPair.getPublic().getEncoded());
            savePem(privatePath, "PRIVATE KEY", keyPair.getPrivate().getEncoded());

            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(keyId)
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to load or generate RSA key pair", exception);
        }
    }

    private RSAKey loadRsaKey(String keyId, Path publicPath, Path privatePath) throws Exception {
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(readPem(publicPath, "PUBLIC KEY")));
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(readPem(privatePath, "PRIVATE KEY")));

        return new RSAKey.Builder((RSAPublicKey) publicKey)
                .privateKey((RSAPrivateKey) privateKey)
                .keyID(keyId)
                .build();
    }

    private void savePem(Path path, String type, byte[] derBytes) throws Exception {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String base64 = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(derBytes);
        String pem = "-----BEGIN " + type + "-----\n"
                + base64
                + "\n-----END " + type + "-----\n";

        Files.writeString(path, pem,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }

    private byte[] readPem(Path path, String type) throws Exception {
        String pem = Files.readString(path)
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s+", "");
        return Base64.getDecoder().decode(pem);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(RSAKey rsaKey) {
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder(RSAKey rsaKey) throws JOSEException {
        return NimbusJwtDecoder.withPublicKey(rsaKey.toRSAPublicKey()).build();
    }
}

