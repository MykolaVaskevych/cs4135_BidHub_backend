package com.bidhub.account.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalApiTokenFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Internal-Token";
    private static final String SERVICE_PRINCIPAL = "SYSTEM";
    private static final SimpleGrantedAuthority ROLE_INTERNAL =
            new SimpleGrantedAuthority("ROLE_INTERNAL");

    private final String expectedToken;

    public InternalApiTokenFilter(@Value("${bidhub.internal.api-token:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!StringUtils.hasText(expectedToken)) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"INTERNAL_API_TOKEN is not configured on this service\"}");
            return;
        }

        String presented = request.getHeader(HEADER);
        if (!expectedToken.equals(presented)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // Valid token authenticates the caller as an internal service principal so the request
        // passes Spring Security's anyRequest().authenticated() check. HeaderAuthenticationFilter
        // runs after this one and may override the principal with a real user identity when the
        // gateway forwards X-User-Id; that's the correct behaviour for user-initiated calls.
        // Service-to-service calls (only X-Internal-Token, no X-User-Id) keep the SYSTEM
        // principal, so individual callers do not need to forge a user identity.
        UsernamePasswordAuthenticationToken serviceAuth =
                new UsernamePasswordAuthenticationToken(
                        SERVICE_PRINCIPAL, null, List.of(ROLE_INTERNAL));
        SecurityContextHolder.getContext().setAuthentication(serviceAuth);

        chain.doFilter(request, response);
    }
}
