package com.changzheng.gateway.filter;

import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * JWT认证全局过滤器
 */
@Slf4j
@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final SecretKey secretKey;

    public JwtAuthFilter(@Value("${jwt.secret}") String jwtSecret) {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 白名单路径(无需认证)
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/wx/login",
            "/api/auth/refresh",
            "/api/admin/login",
            "/api/common/config",
            "/api/common/banners",
            "/doc.html",
            "/webjars/",
            "/swagger-resources",
            "/swagger-resources/",
            "/swagger-ui/",
            "/v3/api-docs",
            "/v3/api-docs/"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 预检和白名单请求不需要令牌，但仍要丢弃客户端伪造的内部身份头。
        if ("OPTIONS".equalsIgnoreCase(request.getMethod().name()) || isWhiteListed(path)) {
            return chain.filter(withSanitizedIdentityHeaders(exchange));
        }

        // 获取Token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isBlank(authHeader) || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "未提供认证Token");
        }

        String token = authHeader.substring(7);

        try {
            // 解析Token
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 检查token类型
            String tokenType = claims.get("tokenType", String.class);
            if (!"access".equals(tokenType)) {
                return unauthorized(exchange, "Token类型错误");
            }

            // 将用户信息传递给下游服务
            String userId = claims.getSubject();
            String userType = claims.get("userType", String.class);
            String role = claims.get("role", String.class);
            if (!isPositiveNumericId(userId)) {
                return unauthorized(exchange, "Token身份无效");
            }

            if (path.startsWith("/api/admin/") || path.startsWith("/api/content/file/")) {
                if (!"ADMIN".equals(userType) || !("ADMIN".equals(role) || "SUPER_ADMIN".equals(role))) {
                    return forbidden(exchange, "Administrator access required");
                }
            } else if (!"STUDENT".equals(userType)) {
                return forbidden(exchange, "Student access required");
            }

            ServerHttpRequest modifiedRequest = request.mutate()
                    .headers(headers -> {
                        headers.remove("X-User-Id");
                        headers.remove("X-User-Type");
                        headers.remove("X-Admin-Id");
                        headers.set("X-User-Type", userType);
                        if ("ADMIN".equals(userType)) {
                            headers.set("X-Admin-Id", userId);
                        } else {
                            headers.set("X-User-Id", userId);
                        }
                    })
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("Rejected expired bearer token for {}", path);
            return unauthorized(exchange, "Token已过期");
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Rejected invalid bearer token for {}", path);
            return unauthorized(exchange, "Token无效");
        }
    }

    private ServerWebExchange withSanitizedIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove("X-User-Id");
                    headers.remove("X-User-Type");
                    headers.remove("X-Admin-Id");
                })
                .build();
        return exchange.mutate().request(sanitizedRequest).build();
    }

    private boolean isPositiveNumericId(String subject) {
        try {
            return subject != null && Long.parseLong(subject) > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(item -> item.endsWith("/") ? path.startsWith(item) : path.equals(item));
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":40300,\"message\":\"%s\",\"timestamp\":%d}",
                message, System.currentTimeMillis());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"code\":40100,\"message\":\"%s\",\"timestamp\":%d}",
                message, System.currentTimeMillis());
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
