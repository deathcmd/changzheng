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

    @Value("${jwt.secret:changzheng-cloud-march-secret-key-2024-very-long}")
    private String jwtSecret;

    /**
     * 白名单路径(无需认证)
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/wx/login",
            "/api/auth/refresh",
            "/api/admin/login",
            "/doc.html",
            "/webjars/",
            "/swagger-resources",
            "/v3/api-docs"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 白名单放行
        if (isWhiteListed(path)) {
            return chain.filter(exchange);
        }

        // 获取Token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StrUtil.isBlank(authHeader) || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "未提供认证Token");
        }

        String token = authHeader.substring(7);

        try {
            // 解析Token
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parser()
                    .verifyWith(key)
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

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Type", userType)
                    .build();

            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            return unauthorized(exchange, "Token已过期");
        } catch (JwtException e) {
            log.warn("Token解析失败: {}", e.getMessage());
            return unauthorized(exchange, "Token无效");
        }
    }

    private boolean isWhiteListed(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
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
