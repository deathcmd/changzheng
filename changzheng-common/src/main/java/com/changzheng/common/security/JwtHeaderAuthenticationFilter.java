package com.changzheng.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.List;

/**
 * Verifies bearer tokens again in each servlet service and replaces identity
 * headers with claims from the verified token. This prevents callers that can
 * reach an internal service from spoofing X-User-Id or X-Admin-Id.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JwtHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_TYPE_HEADER = "X-User-Type";
    private static final String ADMIN_ID_HEADER = "X-Admin-Id";

    private final SecretKey secretKey;
    private final IdentityStatusVerifier identityStatusVerifier;

    @Autowired
    public JwtHeaderAuthenticationFilter(@Value("${jwt.secret}") String jwtSecret,
                                         IdentityStatusVerifier identityStatusVerifier) {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must contain at least 32 bytes");
        }
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.identityStatusVerifier = identityStatusVerifier;
    }

    JwtHeaderAuthenticationFilter(String jwtSecret) {
        this(jwtSecret, null);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        SecurityContextHolder.clearContext();
        MutableHeadersRequest sanitizedRequest = new MutableHeadersRequest(request);
        sanitizedRequest.removeHeader(USER_ID_HEADER);
        sanitizedRequest.removeHeader(USER_TYPE_HEADER);
        sanitizedRequest.removeHeader(ADMIN_ID_HEADER);

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(sanitizedRequest, response);
            return;
        }

        RequiredIdentity requiredIdentity = requiredIdentity(request.getRequestURI());
        if (requiredIdentity == RequiredIdentity.NONE) {
            filterChain.doFilter(sanitizedRequest, response);
            return;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.length() <= 7) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
            return;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(authorization.substring(7))
                    .getPayload();

            if (!"access".equals(claims.get("tokenType", String.class))) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token type");
                return;
            }

            String userType = claims.get("userType", String.class);
            String subject = claims.getSubject();
            if (!isPositiveNumericId(subject)) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid token identity");
                return;
            }
            long identityId = Long.parseLong(subject);
            if (requiredIdentity == RequiredIdentity.ADMIN) {
                String role = claims.get("role", String.class);
                if (!"ADMIN".equals(userType) || !("ADMIN".equals(role) || "SUPER_ADMIN".equals(role))) {
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, "Administrator access required");
                    return;
                }
                if (identityStatusVerifier != null && !identityStatusVerifier.isActiveAdmin(identityId)) {
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, "Administrator account disabled");
                    return;
                }
                sanitizedRequest.setHeader(ADMIN_ID_HEADER, subject);
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        subject, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
            } else if (!"STUDENT".equals(userType)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Student access required");
                return;
            } else {
                if (identityStatusVerifier != null && !identityStatusVerifier.isActiveStudent(identityId)) {
                    writeError(response, HttpServletResponse.SC_FORBIDDEN, "Student account disabled");
                    return;
                }
                sanitizedRequest.setHeader(USER_ID_HEADER, subject);
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        subject, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
            }
            sanitizedRequest.setHeader(USER_TYPE_HEADER, userType);
            try {
                filterChain.doFilter(sanitizedRequest, response);
            } finally {
                SecurityContextHolder.clearContext();
            }
        } catch (JwtException exception) {
            log.warn("Rejected invalid bearer token for {}", request.getRequestURI());
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
        }
    }

    private RequiredIdentity requiredIdentity(String requestUri) {
        if (requestUri.equals("/admin/login")) {
            return RequiredIdentity.NONE;
        }
        if (requestUri.startsWith("/admin/") || requestUri.startsWith("/content/file/")) {
            return RequiredIdentity.ADMIN;
        }
        if (requestUri.startsWith("/api/sport/")
                || requestUri.startsWith("/api/rank/")
                || requestUri.startsWith("/content/")) {
            return RequiredIdentity.STUDENT;
        }
        if (requestUri.startsWith("/api/auth/")
                && !requestUri.equals("/api/auth/wx/login")
                && !requestUri.equals("/api/auth/refresh")) {
            return RequiredIdentity.STUDENT;
        }
        return RequiredIdentity.NONE;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(String.format("{\"code\":%d,\"message\":\"%s\"}", status, message));
    }

    private boolean isPositiveNumericId(String subject) {
        try {
            return subject != null && Long.parseLong(subject) > 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private enum RequiredIdentity {
        NONE, STUDENT, ADMIN
    }

    private static final class MutableHeadersRequest extends HttpServletRequestWrapper {
        private final Set<String> removedHeaders = new LinkedHashSet<>();
        private final java.util.Map<String, String> replacementHeaders =
                new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        private MutableHeadersRequest(HttpServletRequest request) {
            super(request);
        }

        private void removeHeader(String name) {
            removedHeaders.add(name.toLowerCase(Locale.ROOT));
            replacementHeaders.remove(name);
        }

        private void setHeader(String name, String value) {
            removedHeaders.remove(name.toLowerCase(Locale.ROOT));
            replacementHeaders.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            if (replacementHeaders.containsKey(name)) {
                return replacementHeaders.get(name);
            }
            if (removedHeaders.contains(name.toLowerCase(Locale.ROOT))) {
                return null;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            if (replacementHeaders.containsKey(name)) {
                return Collections.enumeration(Collections.singleton(replacementHeaders.get(name)));
            }
            if (removedHeaders.contains(name.toLowerCase(Locale.ROOT))) {
                return Collections.emptyEnumeration();
            }
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> names = new LinkedHashSet<>();
            Enumeration<String> originalNames = super.getHeaderNames();
            while (originalNames != null && originalNames.hasMoreElements()) {
                String name = originalNames.nextElement();
                if (!removedHeaders.contains(name.toLowerCase(Locale.ROOT))) {
                    names.add(name);
                }
            }
            names.addAll(replacementHeaders.keySet());
            return Collections.enumeration(names);
        }
    }
}
