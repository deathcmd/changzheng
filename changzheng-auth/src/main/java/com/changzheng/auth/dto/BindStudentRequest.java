package com.changzheng.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 绑定学生信息请求
 */
@Data
public class BindStudentRequest {

    @NotBlank(message = "学号不能为空")
    @Pattern(regexp = "^\\d{8,12}$", message = "学号格式不正确")
    private String studentNo;

    @NotBlank(message = "姓名不能为空")
    private String name;

    private String classId;
    
    private String className;

    private String grade;

    private String college;

    private Integer enrollYear;
}
