package com.bidhub.gateway.filter;

import com.bidhub.gateway.config.JwtProperties;
import com.bidhub.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import java.util.List;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final List<String> openPaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthenticationFilter(JwtUtil jwtUtil, JwtProperties properties) {
        this.jwtUtil = jwtUtil;
        this.openPaths = properties.getAuth().getOpenPaths();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (isOpen(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.isValid(token)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        Claims claims = jwtUtil.parseToken(token);
        ServerHttpRequest mutated =
                exchange.getRequest()
                        .mutate()
                        .header("X-User-Id", claims.getSubject())
                        .header("X-User-Roles", claims.get("roles", String.class))
                        .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isOpen(String path) {
        return openPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
