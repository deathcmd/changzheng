package com.changzheng.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.changzheng.admin.dto.StudentImportResult;
import com.changzheng.admin.dto.StudentQueryDTO;
import com.changzheng.admin.dto.StudentStatsDTO;
import com.changzheng.admin.service.StudentService;
import com.changzheng.common.entity.StudentInfo;
import com.changzheng.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

/**
 * 学生管理控制器
 */
@Tag(name = "学生管理")
@RestController
@RequestMapping("/admin/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @Operation(summary = "获取学生列表")
    @GetMapping
    public R<IPage<StudentInfo>> getStudentList(@Valid StudentQueryDTO query) {
        IPage<StudentInfo> page = studentService.getStudentList(query);
        return R.ok(page);
    }

    @Operation(summary = "获取统计数据")
    @GetMapping("/stats")
    public R<StudentStatsDTO> getStats() {
        StudentStatsDTO stats = studentService.getStats();
        return R.ok(stats);
    }

    @Operation(summary = "导入学生数据")
    @PostMapping("/import")
    public R<StudentImportResult> importStudents(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return R.fail("请选择要导入的文件");
        }
        StudentImportResult result = studentService.importStudents(file);
        return R.ok("导入成功", result);
    }

    @Operation(summary = "下载学生导入模板")
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("学生信息导入模板.xlsx", StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok()
                .headers(headers)
                .body(studentService.createImportTemplate());
    }

    @Operation(summary = "更新学生信息")
    @PutMapping("/{id}")
    public R<Void> updateStudent(@PathVariable("id") Long id, @RequestBody StudentInfo student) {
        studentService.updateStudent(id, student);
        return R.ok("更新成功", null);
    }

    @Operation(summary = "停用学生")
    @DeleteMapping("/{id}")
    public R<Void> deleteStudent(@PathVariable("id") Long id) {
        studentService.deleteStudent(id);
        return R.ok("停用成功", null);
    }

    @Operation(summary = "解绑学生（管理员特殊操作）")
    @PostMapping("/{id}/unbind")
    public R<Void> unbindStudent(@PathVariable("id") Long id) {
        studentService.unbindStudent(id);
        return R.ok("解绑成功", null);
    }
}
