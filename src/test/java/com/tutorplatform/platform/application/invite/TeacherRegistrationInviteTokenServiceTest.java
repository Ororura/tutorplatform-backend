package com.tutorplatform.platform.application.invite;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TeacherRegistrationInviteTokenServiceTest {

    private final TeacherRegistrationInviteTokenService service =
        new TeacherRegistrationInviteTokenService();

    @Test
    void generatesUniqueTokens() {
        var first = service.createToken();
        var second = service.createToken();

        assertThat(first.rawValue())
            .isNotEqualTo(second.rawValue());

        assertThat(first.hash())
            .isNotEqualTo(second.hash());
    }

    @Test
    void generatesUrlSafeToken() {
        var token = service.createToken();

        assertThat(token.rawValue())
            .matches("^[A-Za-z0-9_-]{43}$");
    }

    @Test
    void generatesSha256Hash() {
        var token = service.createToken();

        assertThat(token.hash())
            .matches("^[0-9a-f]{64}$");

        assertThat(token.hash())
            .isEqualTo(service.hash(token.rawValue()));
    }

    @Test
    void neverUsesRawTokenAsHash() {
        var token = service.createToken();

        assertThat(token.hash())
            .isNotEqualTo(token.rawValue());
    }

    @Test
    void sameTokenAlwaysProducesSameHash() {
        String rawToken = "example-token";

        assertThat(service.hash(rawToken))
            .isEqualTo(service.hash(rawToken));
    }
}
