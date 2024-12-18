package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PointControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PointService pointService;

    @DisplayName("특정 회원의 포인트를 조회한다.")
    @Test
    void getUserPoint_thenSuccessful() throws Exception {
        // given
        long userId = 1L;
        pointService.chargePoint(userId, 1000L);

        // when //then
        mockMvc.perform(
                get("/point/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(1000))
                .andDo(print());

    }

    @DisplayName("특정 회원의 포인트 내역 목록을 조회한다.")
    @Test
    void getUserHistories_thenSuccessful() throws Exception {
        long userId = 2L;
        pointService.chargePoint(userId, 1500L);
        pointService.usePoint(userId, 1000L);

        // when //then
        mockMvc.perform(
                get("/point/{id}/histories", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].amount").value(1500L))
                .andExpect(jsonPath("$[0].type").value("CHARGE"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].userId").value(userId))
                .andExpect(jsonPath("$[1].amount").value(1000L))
                .andExpect(jsonPath("$[1].type").value("USE"))
                .andDo(print());
    }

    @DisplayName("포인트 충전에 성공한다.")
    @Test
    void chargePoint_thenSuccessful() throws Exception {
        long userId = 3L;

        // when //then
        mockMvc.perform(
                patch("/point/{id}/charge", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(1500L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(1500L))
                .andDo(print());
    }

    @DisplayName("포인트 사용에 성공한다.")
    @Test
    void usePoint_thenSuccessful() throws Exception {
        long userId = 4L;
        long useAmount = 1600L;
        UserPoint initialPoint = pointService.chargePoint(userId, 5000L);

        // when //then
        mockMvc.perform(
                patch("/point/{id}/use", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.valueOf(useAmount)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.point").value(initialPoint.point() - useAmount))
                .andDo(print());
    }

}