package com.changzheng.admin.service;

import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelUtil;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 学生信息服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

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
        StudentImportResult result = new StudentImportResult();
        String batchNo = UUID.randomUUID().toString().substring(0, 8);
        
        try (InputStream inputStream = file.getInputStream()) {
            ExcelReader reader = ExcelUtil.getReader(inputStream);
            List<Map<String, Object>> rows = reader.readAll();
            
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
                    
                    // 查找是否已存在
                    StudentInfo existing = studentInfoMapper.selectByStudentNo(studentNo);
                    
                    if (existing != null) {
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
                } catch (Exception e) {
                    log.error("导入学生数据失败: {}", e.getMessage());
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
            
        } catch (Exception e) {
            log.error("导入学生数据异常", e);
            throw new BusinessException("导入失败: " + e.getMessage());
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
        
        existing.setName(student.getName());
        existing.setGender(student.getGender());
        existing.setMajor(student.getMajor());
        existing.setClassName(student.getClassName());
        existing.setGrade(student.getGrade());
        existing.setPhone(student.getPhone());
        
        studentInfoMapper.updateById(existing);
    }

    /**
     * 删除学生
     */
    public void deleteStudent(Long id) {
        StudentInfo existing = studentInfoMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("学生不存在");
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
        
        // 1. 清除用户表的学号绑定
        studentInfoMapper.clearUserStudentNo(existing.getStudentNo());
        
        // 2. 清除学生信息表的绑定状态
        existing.setIsBound(0);
        existing.setBoundUserId(null);
        existing.setBoundAt(null);
        studentInfoMapper.updateById(existing);
        
        log.warn("管理员解绑学生: studentId={}, studentNo={}, 原绑定用户ID={}", 
                id, existing.getStudentNo(), existing.getBoundUserId());
    }

    private String getStringValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        return value.toString().trim();
    }
}
