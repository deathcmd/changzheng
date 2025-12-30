package com.changzheng.sport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.changzheng.common.entity.DailySteps;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日步数Mapper
 */
@Mapper
public interface DailyStepsMapper extends BaseMapper<DailySteps> {

    @Select("SELECT * FROM t_daily_steps WHERE user_id = #{userId} AND record_date = #{date} LIMIT 1")
    DailySteps selectByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    @Select("SELECT * FROM t_daily_steps WHERE user_id = #{userId} AND record_date BETWEEN #{startDate} AND #{endDate} ORDER BY record_date DESC")
    List<DailySteps> selectByUserIdAndDateRange(@Param("userId") Long userId, 
                                                  @Param("startDate") LocalDate startDate, 
                                                  @Param("endDate") LocalDate endDate);

    @Select("SELECT COALESCE(SUM(valid_steps), 0) FROM t_daily_steps WHERE user_id = #{userId}")
    Long sumValidStepsByUserId(@Param("userId") Long userId);
}
