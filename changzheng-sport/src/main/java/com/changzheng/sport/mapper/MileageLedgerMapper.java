package com.changzheng.sport.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.changzheng.common.entity.MileageLedger;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;

/**
 * 里程流水Mapper
 */
@Mapper
public interface MileageLedgerMapper extends BaseMapper<MileageLedger> {

    @Select("SELECT COALESCE(SUM(mileage_delta), 0) FROM t_mileage_ledger WHERE user_id = #{userId} AND status = 1")
    BigDecimal sumMileageByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM t_mileage_ledger WHERE user_id = #{userId} AND status = 1 ORDER BY record_date DESC LIMIT 1")
    MileageLedger selectLatestByUserId(@Param("userId") Long userId);
}
