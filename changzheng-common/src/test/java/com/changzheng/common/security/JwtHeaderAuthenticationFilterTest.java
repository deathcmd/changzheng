package com.changzheng.common.security;

import com.changzheng.common.util.JwtUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtHeaderAuthenticationFilterTest {

    private static final String SECRET = "test".repeat(8);
    private final JwtHeaderAuthenticationFilter filter = new JwtHeaderAuthenticationFilter(SECRET);
    private final JwtUtils jwtUtils = new JwtUtils(SECRET, 60_000, 60_000);

    @Test
    void replacesSpoofedIdentityHeadersWithVerifiedStudentClaims() throws Exception {
        MockHttpServletRequest request = request("/api/sport/progress");
        request.addHeader("Authorization", "Bearer " + jwtUtils.generateAccessToken(42L, "STUDENT", Map.of()));
        request.addHeader("X-User-Id", "999");
        request.addHeader("X-Admin-Id", "1");
        AtomicReference<HttpServletRequest> forwarded = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(),
                (servletRequest, servletResponse) -> forwarded.set((HttpServletRequest) servletRequest));

        assertEquals("42", forwarded.get().getHeader("X-User-Id"));
        assertEquals("STUDENT", forwarded.get().getHeader("X-User-Type"));
        assertNull(forwarded.get().getHeader("X-Admin-Id"));
    }

    @Test
    void rejectsStudentTokenForAdminApi() throws Exception {
        MockHttpServletRequest request = request("/admin/info");
        request.addHeader("Authorization", "Bearer " + jwtUtils.generateAccessToken(42L, "STUDENT", Map.of()));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> { });

        assertEquals(403, response.getStatus());
    }

    @Test
    void rejectsAdminTokenForStudentApi() throws Exception {
        MockHttpServletRequest request = request("/api/rank/my");
        request.addHeader("Authorization", "Bearer " + adminToken(7L));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> { });

        assertEquals(403, response.getStatus());
    }

    @Test
    void rejectsRefreshTokenForProtectedApi() throws Exception {
        MockHttpServletRequest request = request("/api/auth/userInfo");
        request.addHeader("Authorization", "Bearer " + jwtUtils.generateRefreshToken(42L, "STUDENT"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> { });

        assertEquals(401, response.getStatus());
    }

    @Test
    void requiresStudentTokenForContentApi() throws Exception {
        MockHttpServletRequest request = request("/content/detail/7");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> { });

        assertEquals(401, response.getStatus());
    }

    @Test
    void rejectsAccessTokenAfterStudentIsDisabled() throws Exception {
        IdentityStatusVerifier verifier = mock(IdentityStatusVerifier.class);
        when(verifier.isActiveStudent(42L)).thenReturn(false);
        JwtHeaderAuthenticationFilter statusAwareFilter =
                new JwtHeaderAuthenticationFilter(SECRET, verifier);
        MockHttpServletRequest request = request("/api/sport/progress");
        request.addHeader("Authorization",
                "Bearer " + jwtUtils.generateAccessToken(42L, "STUDENT", Map.of()));
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] invoked = {false};

        statusAwareFilter.doFilter(request, response,
                (servletRequest, servletResponse) -> invoked[0] = true);

        assertEquals(403, response.getStatus());
        assertFalse(invoked[0]);
        verify(verifier).isActiveStudent(42L);
    }

    @Test
    void rejectsAccessTokenAfterAdministratorIsDisabled() throws Exception {
        IdentityStatusVerifier verifier = mock(IdentityStatusVerifier.class);
        when(verifier.isActiveAdmin(7L)).thenReturn(false);
        JwtHeaderAuthenticationFilter statusAwareFilter =
                new JwtHeaderAuthenticationFilter(SECRET, verifier);
        MockHttpServletRequest request = request("/admin/info");
        request.addHeader("Authorization", "Bearer " + adminToken(7L));
        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean[] invoked = {false};

        statusAwareFilter.doFilter(request, response,
                (servletRequest, servletResponse) -> invoked[0] = true);

        assertEquals(403, response.getStatus());
        assertFalse(invoked[0]);
        verify(verifier).isActiveAdmin(7L);
    }

    @Test
    void sanitizesIdentityHeadersOnCorsPreflight() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/sport/progress");
        request.setRequestURI("/api/sport/progress");
        request.addHeader("X-User-Id", "999");
        request.addHeader("X-Admin-Id", "1");
        AtomicReference<HttpServletRequest> forwarded = new AtomicReference<>();

        filter.doFilter(request, new MockHttpServletResponse(),
                (servletRequest, servletResponse) -> forwarded.set((HttpServletRequest) servletRequest));

        assertNull(forwarded.get().getHeader("X-User-Id"));
        assertNull(forwarded.get().getHeader("X-Admin-Id"));
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.setRequestURI(path);
        return request;
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
}
