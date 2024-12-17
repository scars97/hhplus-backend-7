package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

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
        long amount = 1000L;
        long currentTime = System.currentTimeMillis();

        long initialPoint = 5000L;
        long chargedPoint = initialPoint + amount;
        UserPoint mockUserPoint = new UserPoint(userId, initialPoint, currentTime);
        PointHistory mockPointHistory = new PointHistory(1L, userId, amount, TransactionType.CHARGE, currentTime);

        when(userPointTable.selectById(userId)).thenReturn(mockUserPoint);
        /*
         * 포인트 내역 저장 시간은 pointHistoryTable.insert()를 호출할 때 값이 결정된다.
         * 테스트의 경우 currentTime 이라는 지정된 값을 전달했기 때문에 시간 값 불일치로 실패한다.
         * ArgumentCaptor 를 사용해서 메서드 호출 시 결정된 값을 캡처하여 검증한다.
         */
        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
        when(pointHistoryTable.insert(eq(userId), eq(amount), eq(TransactionType.CHARGE), timeCaptor.capture())).thenReturn(mockPointHistory);
        when(userPointTable.insertOrUpdate(userId, chargedPoint)).thenReturn(new UserPoint(userId, chargedPoint, currentTime));

        // when
        UserPoint updatedPoint = pointService.chargePoint(userId, amount);

        // then
        Long capturedTime = timeCaptor.getValue();
        assertThat(updatedPoint)
                .extracting("id", "point", "updateMillis")
                .containsExactly(userId, chargedPoint, currentTime);
        verify(userPointTable).selectById(userId);
        verify(pointHistoryTable).insert(userId, amount, TransactionType.CHARGE, capturedTime);
        verify(userPointTable).insertOrUpdate(userId, initialPoint + amount);
    }

    @DisplayName("포인트 충전 시, 입력된 값이 1000 미만인 경우 예외가 발생한다.")
    @Test
    void chargePoint_shouldThrowException_whenAmountIsLessThan1000() {
        // given
        long id = 1L;
        long invalidAmount = 999L;

        // when //then
        assertThatThrownBy(() -> pointService.chargePoint(id, invalidAmount))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("포인트 충전은 1,000원 이상부터 가능합니다.");
    }

    @DisplayName("포인트 사용 시, 포인트가 감소하고 내역이 저장된다.")
    @Test
    void usedPoint_shouldReducePointAndSavePointHistory() {
        // given
        long userId = 1L;
        long amount = 1000L;
        long currentTime = System.currentTimeMillis();

        long initialPoint = 5000L;
        long remainingPoint = initialPoint - amount;
        UserPoint mockUserPoint = new UserPoint(userId, initialPoint, currentTime);
        PointHistory mockPointHistory = new PointHistory(1L, userId, amount, TransactionType.USE, currentTime);

        when(userPointTable.selectById(userId)).thenReturn(mockUserPoint);
        ArgumentCaptor<Long> timeCaptor = ArgumentCaptor.forClass(Long.class);
        when(pointHistoryTable.insert(eq(userId), eq(amount), eq(TransactionType.USE), timeCaptor.capture())).thenReturn(mockPointHistory);
        when(userPointTable.insertOrUpdate(userId, remainingPoint)).thenReturn(new UserPoint(userId, remainingPoint, currentTime));

        // when
        UserPoint remainingUserPoint = pointService.usePoint(userId, amount);

        //then
        Long capturedTime = timeCaptor.getValue();
        assertThat(remainingUserPoint)
                .extracting("id", "point", "updateMillis")
                .containsExactly(userId, remainingPoint, currentTime);
        verify(userPointTable).selectById(userId);
        verify(pointHistoryTable).insert(userId, amount, TransactionType.USE, capturedTime);
        verify(userPointTable).insertOrUpdate(userId, remainingPoint);
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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔고 부족");
        verify(userPointTable).selectById(userId);
    }
}