package com.changzheng.sport.service;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.changzheng.common.entity.DailySteps;
import com.changzheng.common.entity.MileageLedger;
import com.changzheng.common.entity.User;
import com.changzheng.sport.dto.SyncResult;
import com.changzheng.sport.mapper.DailyStepsMapper;
import com.changzheng.sport.mapper.MileageLedgerMapper;
import com.changzheng.sport.mapper.RouteNodeMapper;
import com.changzheng.sport.mapper.UserMapper;
import com.changzheng.sport.mapper.UserNodeProgressMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StepSyncServiceTest {

    @Mock private DailyStepsMapper dailyStepsMapper;
    @Mock private MileageLedgerMapper mileageLedgerMapper;
    @Mock private UserNodeProgressMapper userNodeProgressMapper;
    @Mock private RouteNodeMapper routeNodeMapper;
    @Mock private UserMapper userMapper;

    private StepSyncService service;

    @BeforeEach
    void setUp() {
        service = new StepSyncService(dailyStepsMapper, mileageLedgerMapper, userNodeProgressMapper,
                routeNodeMapper, userMapper);
        ReflectionTestUtils.setField(service, "stepToKmRate", 2_000);
        ReflectionTestUtils.setField(service, "dailyStepLimit", 30_000);
        ReflectionTestUtils.setField(service, "anomalyThreshold", 50_000);
        ReflectionTestUtils.setField(service, "totalDistance", BigDecimal.valueOf(25_000));
        ReflectionTestUtils.setField(service, "maxSyncRecords", 31);
    }

    @Test
    void accumulatesExistingDailyLedgerAndReturnsPersistedTodayTotals() {
        User user = activeUser();
        DailySteps today = new DailySteps();
        today.setId(10L);
        today.setUserId(1L);
        today.setRecordDate(LocalDate.now());
        today.setRawSteps(1_000);
        today.setValidSteps(1_000);

        when(userMapper.selectByIdForUpdate(1L)).thenReturn(user);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(dailyStepsMapper.selectByUserIdAndDate(1L, LocalDate.now())).thenReturn(today);
        when(dailyStepsMapper.sumValidStepsByUserId(1L)).thenReturn(2_000L);
        when(routeNodeMapper.selectList(any())).thenReturn(List.of());

        SyncResult result = service.syncSteps(1L, stepsForToday(2_000));

        assertEquals(1, result.getSyncCount());
        assertEquals(2_000, result.getTodaySteps());
        assertEquals(new BigDecimal("1.00"), result.getTodayMileage());
        assertEquals(new BigDecimal("1.00"), result.getTotalMileage());
        assertEquals(2_000L, user.getTotalSteps());
        ArgumentCaptor<MileageLedger> ledgerCaptor = ArgumentCaptor.forClass(MileageLedger.class);
        verify(mileageLedgerMapper).insert(ledgerCaptor.capture());
        assertEquals(1_000, ledgerCaptor.getValue().getSteps());
        assertEquals(new BigDecimal("0.50"), ledgerCaptor.getValue().getMileageDelta());
    }

    @Test
    void propagatesPersistenceFailureInsteadOfReturningFalseSuccess() {
        User user = activeUser();
        when(userMapper.selectByIdForUpdate(1L)).thenReturn(user);
        when(dailyStepsMapper.selectByUserIdAndDate(1L, LocalDate.now()))
                .thenThrow(new IllegalStateException("database unavailable"));

        assertThrows(IllegalStateException.class, () -> service.syncSteps(1L, stepsForToday(2_000)));

        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void returnsZeroDefaultsWhenPersistedAggregatesAreNull() {
        User user = activeUser();
        user.setTotalMileage(null);
        user.setTotalSteps(null);
        user.setContinuousDays(null);

        when(userMapper.selectById(1L)).thenReturn(user);
        when(dailyStepsMapper.countDaysByUserId(1L)).thenReturn(null);
        when(dailyStepsMapper.selectByUserIdAndDate(1L, LocalDate.now())).thenReturn(null);
        when(userNodeProgressMapper.countUnlockedByUserId(1L)).thenReturn(null);
        when(routeNodeMapper.selectCount(any())).thenReturn(null);

        var progress = service.getProgress(1L);

        assertEquals(BigDecimal.ZERO, progress.getTotalMileage());
        assertEquals(0L, progress.getTotalSteps());
        assertEquals(0, progress.getContinuousDays());
        assertEquals(0, progress.getTotalDays());
        assertEquals(0, progress.getUnlockedNodeCount());
        assertEquals(0, progress.getTotalNodeCount());
    }

    @Test
    void writesLedgerEntriesInChronologicalOrder() {
        User user = activeUser();
        when(userMapper.selectByIdForUpdate(1L)).thenReturn(user);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(dailyStepsMapper.selectByUserIdAndDate(any(), any())).thenReturn(null);
        when(dailyStepsMapper.sumValidStepsByUserId(1L)).thenReturn(2_000L);
        when(routeNodeMapper.selectList(any())).thenReturn(List.of());

        long today = LocalDate.now().atTime(LocalTime.NOON)
                .atZone(ZoneId.of("Asia/Shanghai")).toEpochSecond();
        long yesterday = LocalDate.now().minusDays(1).atTime(LocalTime.NOON)
                .atZone(ZoneId.of("Asia/Shanghai")).toEpochSecond();
        JSONArray reverseOrder = JSONUtil.parseArray("[{\"timestamp\":" + today
                + ",\"step\":1000},{\"timestamp\":" + yesterday + ",\"step\":1000}]");

        service.syncSteps(1L, reverseOrder);

        ArgumentCaptor<MileageLedger> captor = ArgumentCaptor.forClass(MileageLedger.class);
        verify(mileageLedgerMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(LocalDate.now().minusDays(1), captor.getAllValues().get(0).getRecordDate());
        assertEquals(LocalDate.now(), captor.getAllValues().get(1).getRecordDate());
        assertEquals(new BigDecimal("0.50"), captor.getAllValues().get(0).getMileageBefore());
        assertEquals(new BigDecimal("1.00"), captor.getAllValues().get(1).getMileageBefore());
    }

    private User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setStatus(1);
        user.setCreatedAt(LocalDateTime.now().minusDays(2));
        user.setTotalMileage(new BigDecimal("0.50"));
        user.setTotalSteps(1_000L);
        return user;
    }

    private JSONArray stepsForToday(int steps) {
        long timestamp = LocalDate.now().atTime(LocalTime.NOON)
                .atZone(ZoneId.of("Asia/Shanghai")).toEpochSecond();
        return JSONUtil.parseArray("[{\"timestamp\":" + timestamp +
                ",\"step\":" + steps + "}]");
    }
}
