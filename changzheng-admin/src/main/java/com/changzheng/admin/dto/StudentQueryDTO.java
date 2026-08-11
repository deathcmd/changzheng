package com.changzheng.admin.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 学生查询DTO
 */
@Data
public class StudentQueryDTO {

    /**
     * 页码
     */
    @Min(value = 1, message = "页码必须大于0")
    private Integer page = 1;

    /**
     * 每页数量
     */
    @Min(value = 1, message = "每页数量必须大于0")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 20;

    /**
     * 关键词（学号/姓名）
     */
    @Size(max = 64, message = "关键词长度不能超过64个字符")
    private String keyword;

    /**
     * 专业
     */
    @Size(max = 64, message = "专业长度不能超过64个字符")
    private String major;

    /**
     * 班级
     */
    @Size(max = 64, message = "班级长度不能超过64个字符")
    private String className;

    /**
     * 绑定状态：0-未绑定 1-已绑定
     */
    private Integer isBound;
}
