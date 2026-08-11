package com.changzheng.admin.service;

import com.changzheng.admin.dto.AdminLoginDTO;
import com.changzheng.admin.mapper.AdminMapper;
import com.changzheng.common.exception.BusinessException;
import com.changzheng.common.util.RedisUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock private AdminMapper adminMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RedisUtils redisUtils;

    @Test
    void unknownUserRunsDummyHashAndAtomicallyRecordsFailure() {
        AdminLoginDTO request = new AdminLoginDTO();
        request.setUsername("missing-admin");
        request.setPassword("wrong-password");
        when(redisUtils.incrementWithExpiry(anyString(), eq(15L), eq(TimeUnit.MINUTES))).thenReturn(1L);
        when(adminMapper.selectOne(any())).thenReturn(null);

        AdminAuthService service = new AdminAuthService(adminMapper, passwordEncoder, redisUtils);

        assertThrows(BusinessException.class, () -> service.login(request));

        verify(passwordEncoder).matches(eq("wrong-password"), anyString());
        verify(redisUtils).incrementWithExpiry(anyString(), eq(15L), eq(TimeUnit.MINUTES));
    }
}
