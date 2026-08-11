package com.changzheng.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtAuthFilterTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private final JwtAuthFilter filter = new JwtAuthFilter(SECRET);

    @Test
    void doesNotExpandExactLoginWhitelist() {
        MockServerWebExchange exchange = exchange("/api/auth/wx/login/extra", null);

        filter.filter(exchange, ignored -> { throw new AssertionError("request must not be forwarded"); }).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void rejectsStudentForAdminApiAndAdminForStudentApi() {
        MockServerWebExchange adminExchange = exchange("/api/admin/info",
                studentToken(42L, "access"));
        filter.filter(adminExchange, ignored -> { throw new AssertionError("request must not be forwarded"); }).block();
        assertEquals(HttpStatus.FORBIDDEN, adminExchange.getResponse().getStatusCode());

        MockServerWebExchange studentExchange = exchange("/api/rank/my", adminToken(7L));
        filter.filter(studentExchange, ignored -> { throw new AssertionError("request must not be forwarded"); }).block();
        assertEquals(HttpStatus.FORBIDDEN, studentExchange.getResponse().getStatusCode());
    }

    @Test
    void rejectsRefreshTokenAndReplacesSpoofedHeaders() {
        MockServerWebExchange refreshExchange = exchange("/api/sport/progress",
                studentToken(42L, "refresh"));
        filter.filter(refreshExchange, ignored -> { throw new AssertionError("request must not be forwarded"); }).block();
        assertEquals(HttpStatus.UNAUTHORIZED, refreshExchange.getResponse().getStatusCode());

        MockServerWebExchange accessExchange = exchange("/api/sport/progress",
                studentToken(42L, "access"));
        AtomicReference<org.springframework.http.server.reactive.ServerHttpRequest> forwarded = new AtomicReference<>();
        filter.filter(accessExchange, forwardedExchange -> {
            forwarded.set(forwardedExchange.getRequest());
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertEquals("42", forwarded.get().getHeaders().getFirst("X-User-Id"));
        assertEquals("STUDENT", forwarded.get().getHeaders().getFirst("X-User-Type"));
        assertNull(forwarded.get().getHeaders().getFirst("X-Admin-Id"));
    }

    @Test
    void sanitizesIdentityHeadersOnWhitelistedRequest() {
        MockServerWebExchange exchange = exchange("/api/auth/wx/login", null);
        AtomicReference<org.springframework.http.server.reactive.ServerHttpRequest> forwarded = new AtomicReference<>();

        filter.filter(exchange, forwardedExchange -> {
            forwarded.set(forwardedExchange.getRequest());
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertNull(forwarded.get().getHeaders().getFirst("X-User-Id"));
        assertNull(forwarded.get().getHeaders().getFirst("X-Admin-Id"));
    }

    private MockServerWebExchange exchange(String path, String token) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get(path)
                .header("X-User-Id", "999")
                .header("X-Admin-Id", "1");
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return MockServerWebExchange.from(builder.build());
    }

    private String adminToken(long adminId) {
        return Jwts.builder()
                .subject(String.valueOf(adminId))
                .claim("userType", "ADMIN")
                .claim("tokenType", "access")
                .claim("role", "SUPER_ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String studentToken(long userId, String tokenType) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userType", "STUDENT")
                .claim("tokenType", tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
