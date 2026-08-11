package com.changzheng.auth.service;

import com.changzheng.auth.dto.BindStudentRequest;
import com.changzheng.auth.mapper.StudentInfoMapper;
import com.changzheng.auth.mapper.UserMapper;
import com.changzheng.common.entity.StudentInfo;
import com.changzheng.common.entity.User;
import com.changzheng.common.exception.BusinessException;
import com.changzheng.common.result.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private WxMiniAppService wxMiniAppService;
    @Mock private UserMapper userMapper;
    @Mock private StudentInfoMapper studentInfoMapper;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(wxMiniAppService, userMapper, studentInfoMapper);
        ReflectionTestUtils.setField(service, "aesKey", "test".repeat(4));
    }

    @Test
    void stopsWhenConcurrentRequestAlreadyClaimedStudent() {
        User user = new User();
        user.setId(1L);
        user.setStatus(1);

        StudentInfo student = new StudentInfo();
        student.setId(10L);
        student.setStudentNo("20220001");
        student.setName("张三");
        student.setIsBound(0);

        BindStudentRequest request = new BindStudentRequest();
        request.setStudentNo(student.getStudentNo());
        request.setName(student.getName());

        when(userMapper.selectByIdForUpdate(1L)).thenReturn(user);
        when(studentInfoMapper.selectByStudentNoAndName("20220001", "张三")).thenReturn(student);
        when(studentInfoMapper.updateBoundStatus(10L, 1L)).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.bindStudent(1L, request));

        assertEquals(ResultCode.STUDENT_ALREADY_BOUND.getCode(), error.getCode());
        verify(userMapper, never()).updateById(any(User.class));
    }
}
