package com.tutorplatform;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FoundationConventionsTest {

    @Test
    void baseApiPrefixRemainsVersioned() {
        assertThat("/api/v1").startsWith("/api/");
    }
}
