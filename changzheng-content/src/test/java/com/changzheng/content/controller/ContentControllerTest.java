package com.changzheng.content.controller;

import com.changzheng.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentControllerTest {

    @Mock private JdbcTemplate jdbcTemplate;

    @Test
    void lockedNodeCannotExposeItsContentList() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenReturn(0);
        ContentController controller = new ContentController(jdbcTemplate);

        assertThrows(BusinessException.class, () -> controller.getNodeContents(7L, 11L));

        verify(jdbcTemplate, never()).queryForList(anyString(), any(Object[].class));
    }
}
