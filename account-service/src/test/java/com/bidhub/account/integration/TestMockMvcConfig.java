package com.bidhub.account.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestMockMvcConfig {

    @Bean
    public MockMvcBuilderCustomizer mockMvcBuilderCustomizer() {
        return builder ->
                builder.defaultRequest(
                        get("/").header("X-Internal-Token", "test-internal-token"));
    }
}
