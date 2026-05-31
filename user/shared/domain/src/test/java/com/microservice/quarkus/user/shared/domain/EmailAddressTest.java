package com.microservice.quarkus.user.shared.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class EmailAddressTest {

    @Test
    void shouldCreateValidEmailAddress() {
        EmailAddress email = new EmailAddress("user@example.com");
        assertEquals("user@example.com", email.value());
        assertEquals("user@example.com", email.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "user@example.com",
            "user.name@example.com",
            "user+tag@example.com",
            "user123@domain.org",
            "UPPER@DOMAIN.COM",
            "a@b.cd"
    })
    void shouldAcceptValidEmails(String validEmail) {
        assertDoesNotThrow(() -> new EmailAddress(validEmail));
    }

    @Test
    void shouldRejectNullEmail() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new EmailAddress(null));
        assertTrue(exception.getMessage().contains("no es válido"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "invalid",
            "invalid@",
            "@domain.com",
            "user@.com",
            "user@domain",
            "user@domain.",
            "user name@domain.com"
    })
    void shouldRejectInvalidEmails(String invalidEmail) {
        assertThrows(IllegalArgumentException.class,
                () -> new EmailAddress(invalidEmail));
    }

    @Test
    void shouldImplementEquality() {
        EmailAddress email1 = new EmailAddress("test@example.com");
        EmailAddress email2 = new EmailAddress("test@example.com");
        EmailAddress email3 = new EmailAddress("other@example.com");

        assertEquals(email1, email2);
        assertEquals(email1.hashCode(), email2.hashCode());
        assertNotEquals(email1, email3);
    }
}
