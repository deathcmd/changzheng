package com.changzheng.rank.service;

import com.changzheng.common.entity.User;
import com.changzheng.common.exception.BusinessException;
import com.changzheng.common.result.ResultCode;
import com.changzheng.rank.dto.PersonalRankDTO;
import com.changzheng.rank.mapper.RankMapper;
import com.changzheng.rank.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RankServiceTest {
    private final RankMapper rankMapper = mock(RankMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final RankService service = new RankService(rankMapper, userMapper);

    @ParameterizedTest
    @CsvSource({"0,20", "-1,20", "1,0", "1,101", "2147483647,100", "21474837,100"})
    void invalidOrOverflowingPaginationIsRejectedBeforeQuery(int page, int size) {
        BusinessException error = assertThrows(BusinessException.class, () -> service.getTotalRank(page, size));
        assertEquals(ResultCode.PARAM_INVALID.getCode(), error.getCode());
        verifyNoInteractions(rankMapper);
    }

    @Test
    void secondPageUsesAnExactOffsetAndRank() {
        PersonalRankDTO record = new PersonalRankDTO();
        when(rankMapper.selectTotalRank(20, 20)).thenReturn(List.of(record));
        when(rankMapper.countTotalRank()).thenReturn(21);
        assertEquals(21, service.getTotalRank(2, 20).get("total"));
        assertEquals(21, record.getRank());
    }

    @Test
    void gradeRankAlsoRejectsOverflow() {
        User user = new User();
        user.setStatus(1);
        user.setGrade("2026");
        when(userMapper.selectById(7L)).thenReturn(user);
        assertThrows(BusinessException.class, () -> service.getGradeRank(7L, Integer.MAX_VALUE, 100));
        verifyNoInteractions(rankMapper);
    }
}
