package com.changzheng.admin.service;

import com.changzheng.admin.mapper.StudentInfoMapper;
import com.changzheng.common.entity.StudentInfo;
import com.changzheng.common.exception.BusinessException;
import cn.hutool.poi.excel.ExcelUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock private StudentInfoMapper studentInfoMapper;

    @Test
    void unbindsUsingAuthoritativeUserIdInsteadOfPlaintextStudentNumber() {
        StudentInfo student = new StudentInfo();
        student.setId(10L);
        student.setStudentNo("20220001");
        student.setIsBound(1);
        student.setBoundUserId(99L);

        when(studentInfoMapper.selectById(10L)).thenReturn(student);
        when(studentInfoMapper.clearUserBinding(99L)).thenReturn(1);

        new StudentService(studentInfoMapper).unbindStudent(10L);

        verify(studentInfoMapper).clearUserBinding(99L);
        ArgumentCaptor<StudentInfo> captor = ArgumentCaptor.forClass(StudentInfo.class);
        verify(studentInfoMapper).updateById(captor.capture());
        assertNull(captor.getValue().getBoundUserId());
        assertNull(captor.getValue().getBoundAt());
    }

    @Test
    void rejectsIdentityChangesForBoundStudent() {
        StudentInfo existing = validStudent();
        existing.setIsBound(1);
        when(studentInfoMapper.selectById(10L)).thenReturn(existing);

        StudentInfo update = validStudent();
        update.setClassName("软件2302");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> new StudentService(studentInfoMapper).updateStudent(10L, update));

        assertTrue(exception.getMessage().contains("请先解绑"));
    }

    @Test
    void createsImportableXlsxTemplate() {
        byte[] template = new StudentService(studentInfoMapper).createImportTemplate();

        assertTrue(template.length > 0);
        try (var reader = ExcelUtil.getReader(new java.io.ByteArrayInputStream(template))) {
            var rows = reader.read();
            assertEquals(3, rows.size());
            assertEquals("学号", rows.get(0).get(0));
            assertEquals("20230001", rows.get(1).get(0).toString());
        }
    }

    private StudentInfo validStudent() {
        StudentInfo student = new StudentInfo();
        student.setId(10L);
        student.setStudentNo("20230001");
        student.setName("张三");
        student.setGender("男");
        student.setMajor("软件技术");
        student.setClassName("软件2301");
        student.setGrade("2023级");
        student.setPhone("13800000001");
        student.setIsBound(0);
        return student;
    }
}
