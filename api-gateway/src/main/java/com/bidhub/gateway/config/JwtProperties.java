package com.bidhub.gateway.config;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "bidhub")
public class JwtProperties {

    private Jwt jwt = new Jwt();
    private Auth auth = new Auth();
    private Internal internal = new Internal();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
    }

    @Getter
    @Setter
    public static class Auth {
        private List<String> openPaths = List.of();
    }

    @Getter
    @Setter
    public static class Internal {
        private String apiToken;
    }
}
