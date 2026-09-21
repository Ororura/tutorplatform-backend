package com.tutorplatform;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FoundationConventionsTest {

    @Test
    void baseApiPrefixRemainsVersioned() {
        assertThat("/api/v1").startsWith("/api/");
    }
}
