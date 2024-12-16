package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class PointServiceIntegrationTest {

    private PointService pointService;
    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();
        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    @DisplayName("포인트 충전 시, 보유 포인트가 증가하고 내역이 저장된다.")
    @Test
    void chargePoint() {
        // given
        long userId = 1L;
        long amount = 1000L;
        UserPoint initialUserPoint = userPointTable.insertOrUpdate(1L, 500L);

        // when
        UserPoint chargedUserPoint = pointService.chargePoint(userId, amount);

        //then
        assertThat(chargedUserPoint.id()).isEqualTo(userId);
        assertThat(chargedUserPoint.point()).isEqualTo(initialUserPoint.point() + amount);

        List<PointHistory> pointHistories = pointService.getPointHistories(userId);
        assertThat(pointHistories).hasSize(1)
            .extracting("id", "userId", "amount", "type")
            .contains(
                tuple(1L, userId, amount, TransactionType.CHARGE)
            );
    }

    @DisplayName("포인트 사용 시, 보유 포인트가 감소하고 내역이 저장된다.")
    @Test
    void usePoint() {
        // given
        long userId = 1L;
        long amount = 1000L;
        UserPoint initialUserPoint = userPointTable.insertOrUpdate(1L, 1500L);

        // when
        UserPoint chargedUserPoint = pointService.usePoint(userId, amount);

        //then
        assertThat(chargedUserPoint.id()).isEqualTo(userId);
        assertThat(chargedUserPoint.point()).isEqualTo(initialUserPoint.point() - amount);

        List<PointHistory> pointHistories = pointService.getPointHistories(userId);
        assertThat(pointHistories).hasSize(1)
            .extracting("id", "userId", "amount", "type")
            .contains(
                tuple(1L, userId, amount, TransactionType.USE)
            );
    }

}