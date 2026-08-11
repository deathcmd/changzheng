package com.changzheng.admin.service;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.changzheng.admin.dto.StudentImportResult;
import com.changzheng.admin.dto.StudentQueryDTO;
import com.changzheng.admin.dto.StudentStatsDTO;
import com.changzheng.admin.mapper.StudentInfoMapper;
import com.changzheng.common.entity.StudentInfo;
import com.changzheng.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;
import java.util.Arrays;
import java.util.Objects;

/**
 * 学生信息服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private static final long MAX_IMPORT_BYTES = 5L * 1024 * 1024;
    private static final int MAX_IMPORT_ROWS = 5_000;

    private final StudentInfoMapper studentInfoMapper;

    /**
     * 分页查询学生列表
     */
    public IPage<StudentInfo> getStudentList(StudentQueryDTO query) {
        Page<StudentInfo> page = new Page<>(query.getPage(), query.getSize());
        
        LambdaQueryWrapper<StudentInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StudentInfo::getStatus, 1);
        
        // 关键词搜索（学号或姓名）
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.and(w -> w
                    .like(StudentInfo::getStudentNo, query.getKeyword())
                    .or()
                    .like(StudentInfo::getName, query.getKeyword()));
        }
        
        // 专业筛选
        if (query.getMajor() != null && !query.getMajor().isEmpty()) {
            wrapper.eq(StudentInfo::getMajor, query.getMajor());
        }
        
        // 班级筛选
        if (query.getClassName() != null && !query.getClassName().isEmpty()) {
            wrapper.eq(StudentInfo::getClassName, query.getClassName());
        }
        
        // 绑定状态筛选
        if (query.getIsBound() != null) {
            wrapper.eq(StudentInfo::getIsBound, query.getIsBound());
        }
        
        wrapper.orderByDesc(StudentInfo::getCreatedAt);
        
        return studentInfoMapper.selectPage(page, wrapper);
    }

    /**
     * 获取统计数据
     */
    public StudentStatsDTO getStats() {
        StudentStatsDTO stats = new StudentStatsDTO();
        stats.setTotal(studentInfoMapper.countTotal());
        stats.setBound(studentInfoMapper.countBound());
        stats.setUnbound(stats.getTotal() - stats.getBound());
        if (stats.getTotal() > 0) {
            stats.setBindRate(Math.round(stats.getBound() * 100.0 / stats.getTotal() * 10) / 10.0);
        } else {
            stats.setBindRate(0.0);
        }
        return stats;
    }

    /**
     * 导入学生数据
     */
    @Transactional
    public StudentImportResult importStudents(MultipartFile file) {
        validateImportFile(file);
        StudentImportResult result = new StudentImportResult();
        String batchNo = UUID.randomUUID().toString().substring(0, 8);
        
        try (InputStream inputStream = file.getInputStream()) {
            ExcelReader reader = ExcelUtil.getReader(inputStream);
            List<Map<String, Object>> rows = reader.readAll();
            if (rows.size() > MAX_IMPORT_ROWS) {
                throw new BusinessException("单次导入不能超过" + MAX_IMPORT_ROWS + "行");
            }
            
            int successCount = 0;
            int updateCount = 0;
            int failCount = 0;
            
            for (Map<String, Object> row : rows) {
                try {
                    String studentNo = getStringValue(row, "学号");
                    String name = getStringValue(row, "姓名");
                    
                    if (studentNo == null || studentNo.isEmpty() || name == null || name.isEmpty()) {
                        failCount++;
                        continue;
                    }
                    validateStudentFields(studentNo, name,
                            getStringValue(row, "性别"), getStringValue(row, "专业"),
                            getStringValue(row, "班级"), getStringValue(row, "年级"),
                            getStringValue(row, "手机号"));
                    
                    // 查找是否已存在
                    StudentInfo existing = studentInfoMapper.selectByStudentNo(studentNo);
                    
                    if (existing != null) {
                        rejectBoundIdentityChange(existing, name, getStringValue(row, "专业"),
                                getStringValue(row, "班级"), getStringValue(row, "年级"));
                        // 更新已有记录
                        existing.setName(name);
                        existing.setGender(getStringValue(row, "性别"));
                        existing.setMajor(getStringValue(row, "专业"));
                        existing.setClassName(getStringValue(row, "班级"));
                        existing.setGrade(getStringValue(row, "年级"));
                        existing.setPhone(getStringValue(row, "手机号"));
                        existing.setImportBatch(batchNo);
                        studentInfoMapper.updateById(existing);
                        updateCount++;
                    } else {
                        // 新增记录
                        StudentInfo student = new StudentInfo();
                        student.setStudentNo(studentNo);
                        student.setName(name);
                        student.setGender(getStringValue(row, "性别"));
                        student.setMajor(getStringValue(row, "专业"));
                        student.setClassName(getStringValue(row, "班级"));
                        student.setGrade(getStringValue(row, "年级"));
                        student.setPhone(getStringValue(row, "手机号"));
                        student.setCollege("智能制造与信息工程学院");
                        student.setIsBound(0);
                        student.setStatus(1);
                        student.setImportBatch(batchNo);
                        
                        // 解析入学年份
                        String grade = student.getGrade();
                        if (grade != null && grade.matches("\\d{4}级")) {
                            student.setEnrollYear(Integer.parseInt(grade.substring(0, 4)));
                        }
                        
                        studentInfoMapper.insert(student);
                        successCount++;
                    }
                } catch (BusinessException e) {
                    log.debug("学生导入行处理失败: batch={}, exceptionType={}",
                            batchNo, e.getClass().getSimpleName());
                    failCount++;
                }
            }
            
            result.setSuccess(true);
            result.setTotalCount(rows.size());
            result.setSuccessCount(successCount);
            result.setUpdateCount(updateCount);
            result.setFailCount(failCount);
            result.setBatchNo(batchNo);
            
            log.info("学生数据导入完成: 批次={}, 总数={}, 新增={}, 更新={}, 失败={}", 
                    batchNo, rows.size(), successCount, updateCount, failCount);
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("导入学生数据异常", e);
            throw new BusinessException("导入失败，请确认文件格式和字段内容");
        }
        
        return result;
    }

    /**
     * 更新学生信息
     */
    public void updateStudent(Long id, StudentInfo student) {
        StudentInfo existing = studentInfoMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("学生不存在");
        }
        validateStudentFields(existing.getStudentNo(), student.getName(), student.getGender(),
                student.getMajor(), student.getClassName(), student.getGrade(), student.getPhone());
        rejectBoundIdentityChange(existing, student.getName(), student.getMajor(),
                student.getClassName(), student.getGrade());
        
        existing.setName(student.getName());
        existing.setGender(student.getGender());
        existing.setMajor(student.getMajor());
        existing.setClassName(student.getClassName());
        existing.setGrade(student.getGrade());
        existing.setPhone(student.getPhone());
        
        studentInfoMapper.updateById(existing);
    }

    /**
     * 生成与导入接口完全一致的 Office Open XML 模板。
     */
    public byte[] createImportTemplate() {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ExcelWriter writer = ExcelUtil.getWriter(true)) {
            writer.write(Arrays.asList(
                    Arrays.asList("学号", "姓名", "性别", "专业", "班级", "年级", "手机号"),
                    Arrays.asList("20230001", "张三", "男", "软件技术", "软件2301", "2023级", "13800000001"),
                    Arrays.asList("20230002", "李四", "女", "计算机应用技术", "计应2301", "2023级", "13800000002")
            ), false);
            writer.flush(output, false);
            return output.toByteArray();
        } catch (Exception e) {
            log.error("生成学生导入模板失败", e);
            throw new BusinessException("模板生成失败，请稍后重试");
        }
    }

    /**
     * 停用学生
     */
    public void deleteStudent(Long id) {
        StudentInfo existing = studentInfoMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("学生不存在");
        }
        if (Integer.valueOf(1).equals(existing.getIsBound())) {
            throw new BusinessException("已绑定学生不能停用，请先解绑");
        }
        
        // 软删除
        existing.setStatus(0);
        studentInfoMapper.updateById(existing);
    }

    /**
     * 解绑学生（仅管理员可操作）
     * 同时清除学生信息表和用户表的绑定关系
     */
    @Transactional
    public void unbindStudent(Long id) {
        StudentInfo existing = studentInfoMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("学生不存在");
        }
        
        if (existing.getIsBound() == 0) {
            throw new BusinessException("该学生尚未绑定微信");
        }
        
        Long boundUserId = existing.getBoundUserId();
        if (boundUserId == null) {
            throw new BusinessException("绑定数据异常，请检查学生记录");
        }

        // 1. Clear the encrypted user-side binding using the authoritative id.
        if (studentInfoMapper.clearUserBinding(boundUserId) != 1) {
            throw new BusinessException("绑定用户不存在，解绑操作已取消");
        }
        
        // 2. 清除学生信息表的绑定状态
        existing.setIsBound(0);
        existing.setBoundUserId(null);
        existing.setBoundAt(null);
        studentInfoMapper.updateById(existing);
        
        log.warn("管理员解绑学生: studentId={}, 原绑定用户ID={}", id, boundUserId);
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的文件");
        }
        if (file.getSize() > MAX_IMPORT_BYTES) {
            throw new BusinessException("导入文件不能超过5MB");
        }
        String filename = file.getOriginalFilename();
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".xlsx") && !lower.endsWith(".xls")) {
            throw new BusinessException("仅支持 .xlsx 或 .xls 文件");
        }
    }

    private void rejectBoundIdentityChange(StudentInfo existing, String name, String major,
                                           String className, String grade) {
        if (!Integer.valueOf(1).equals(existing.getIsBound())) {
            return;
        }
        if (!Objects.equals(existing.getName(), name)
                || !Objects.equals(existing.getMajor(), major)
                || !Objects.equals(existing.getClassName(), className)
                || !Objects.equals(existing.getGrade(), grade)) {
            throw new BusinessException("已绑定学生的姓名、专业、班级或年级不能直接修改，请先解绑");
        }
    }

    private void validateStudentFields(String studentNo, String name, String gender, String major,
                                       String className, String grade, String phone) {
        if (studentNo == null || !studentNo.matches("\\d{8,12}")) {
            throw new BusinessException("学号必须为8至12位数字");
        }
        requireLength(name, "姓名", 1, 64);
        requireLength(gender, "性别", 0, 4);
        requireLength(major, "专业", 0, 64);
        requireLength(className, "班级", 0, 64);
        requireLength(grade, "年级", 0, 16);
        requireLength(phone, "手机号", 0, 20);
    }

    private void requireLength(String value, String label, int min, int max) {
        int length = value == null ? 0 : value.trim().length();
        if (length < min || length > max) {
            throw new BusinessException(label + "长度不符合要求");
        }
    }

    private String getStringValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        return value.toString().trim();
    }
}
