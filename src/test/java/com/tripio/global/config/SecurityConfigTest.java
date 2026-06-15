package com.tripio.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.tripio.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTest extends IntegrationTestSupport {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void passwordEncoderUsesBcrypt() {
        String encodedPassword = passwordEncoder.encode("tripio-password");

        assertThat(passwordEncoder.matches("tripio-password", encodedPassword)).isTrue();
        assertThat(encodedPassword).startsWith("$2");
    }
}
