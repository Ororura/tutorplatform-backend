package com.tutorplatform.platform.application.invite;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

@Component
public class TeacherRegistrationInviteTokenService {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    public Token createToken() {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(randomBytes);

        return new Token(
            rawToken,
            hash(rawToken)
        );
    }

    public String hash(String rawToken) {
        Objects.requireNonNull(rawToken, "rawToken");

        try {
            byte[] digest = MessageDigest
                .getInstance("SHA-256")
                .digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
                );

            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                "SHA-256 is not available",
                exception
            );
        }
    }

    public record Token(
        String rawValue,
        String hash
    ) {
    }
}
