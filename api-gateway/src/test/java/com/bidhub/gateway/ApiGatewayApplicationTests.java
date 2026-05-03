package com.bidhub.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        properties = {
            "JWT_SECRET=test-jwt-secret-minimum-64-characters-for-hs512-signing-key-padding-ok",
            "INTERNAL_API_TOKEN=test-internal-token",
            "eureka.client.enabled=false",
            "eureka.client.register-with-eureka=false",
            "eureka.client.fetch-registry=false"
        })
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {}
}
