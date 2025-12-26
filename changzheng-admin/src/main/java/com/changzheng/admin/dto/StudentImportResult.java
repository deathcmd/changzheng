package com.changzheng.admin.dto;

import lombok.Data;

/**
 * 学生导入结果DTO
 */
@Data
public class StudentImportResult {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 总记录数
     */
    private Integer totalCount;

    /**
     * 新增成功数
     */
    private Integer successCount;

    /**
     * 更新数量
     */
    private Integer updateCount;

    /**
     * 失败数量
     */
    private Integer failCount;

    /**
     * 导入批次号
     */
    private String batchNo;
}
