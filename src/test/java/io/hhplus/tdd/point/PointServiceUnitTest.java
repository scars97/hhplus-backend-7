package io.hhplus.tdd.point;

import io.hhplus.tdd.UserPointException;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PointServiceUnitTest {

    @InjectMocks
    private PointService pointService;
    @Mock
    private UserPointTable userPointTable;
    @Mock
    private PointHistoryTable pointHistoryTable;

    @DisplayName("포인트 충전 시, 포인트가 증가하고 내역이 저장된다.")
    @Test
    void chargePoint_shouldIncreasePointAndSavePointHistory() {
        // given
        long userId = 1L;
        long originalPoint = 1000L;
        long chargePoint = 1000L;
        long currentTime = System.currentTimeMillis();

        when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, originalPoint, currentTime));
        when(userPointTable.insertOrUpdate(userId, originalPoint + chargePoint)).thenReturn(new UserPoint(userId, originalPoint + chargePoint, currentTime));

        // when
        UserPoint userPoint = pointService.chargePoint(userId, chargePoint);

        // then
        assertThat(userPoint)
                .extracting("id", "point", "updateMillis")
                .containsExactly(userId, originalPoint + chargePoint, currentTime);

        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(pointHistoryTable, times(1)).insert(eq(userId), eq(chargePoint), eq(TransactionType.CHARGE), timeCaptor.capture());
    }

    @DisplayName("포인트 충전 시, 입력된 값이 1000 미만인 경우 예외가 발생한다.")
    @Test
    void chargePoint_shouldThrowException_whenAmountIsLessThan1000() {
        // given
        long id = 1L;
        long invalidAmount = 999L;

        // when //then
        assertThatThrownBy(() -> pointService.chargePoint(id, invalidAmount))
                .isInstanceOf(UserPointException.class)
                .hasMessage("포인트 충전은 1,000원 이상부터 가능합니다.");
    }

    @DisplayName("포인트 사용 시, 포인트가 감소하고 내역이 저장된다.")
    @Test
    void usedPoint_shouldReducePointAndSavePointHistory() {
        // given
        long userId = 1L;
        long originalPoint = 1000L;
        long usePoint = 900L;
        long currentTime = System.currentTimeMillis();

        when(userPointTable.selectById(userId)).thenReturn(new UserPoint(userId, originalPoint, currentTime));
        when(userPointTable.insertOrUpdate(userId, originalPoint - usePoint)).thenReturn(new UserPoint(userId, originalPoint - usePoint, currentTime));

        // when
        UserPoint remainingUserPoint = pointService.usePoint(userId, usePoint);

        //then
        assertThat(remainingUserPoint)
                .extracting("id", "point", "updateMillis")
                .containsExactly(userId, originalPoint - usePoint, currentTime);

        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
        verify(pointHistoryTable, times(1)).insert(eq(userId), eq(usePoint), eq(TransactionType.USE), timeCaptor.capture());
    }

    @DisplayName("포인트 사용 시, 입력된 값이 회원의 보유 포인트보다 큰 경우 예외가 발생한다.")
    @Test
    void usePoint_shouldThrowException_whenAmountIsGreaterThanUserPoint() {
        // given
        long userId = 1L;
        long amount = 2000L;
        long initialPoint = 1000L;

        UserPoint mockUserPoint = new UserPoint(userId, initialPoint, System.currentTimeMillis());
        when(userPointTable.selectById(userId)).thenReturn(mockUserPoint);

        // when //then
        assertThatThrownBy(() -> pointService.usePoint(userId, amount))
                .isInstanceOf(UserPointException.class)
                .hasMessage("잔고 부족");
        verify(userPointTable).selectById(userId);
    }
}