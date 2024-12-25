package io.hhplus.tdd.point;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UserPointTest {

    @DisplayName("입력된 값에 의해 포인트가 증가한다.")
    @Test
    void addPointByInputAmount() {
        // given
        long amount = 1500L;
        UserPoint userPoint = new UserPoint(1L, 1000L, System.currentTimeMillis());

        // when
        long chargedPoint = userPoint.addPoint(amount);

        //then
        assertThat(chargedPoint).isEqualTo(userPoint.point() + amount);
    }

    @DisplayName("입력된 값에 의해 포인트가 감소한다.")
    @Test
    void reducePointByInputAmount() {
        // given
        long amount = 1500L;
        UserPoint userPoint = new UserPoint(1L, 2000L, System.currentTimeMillis());

        // when
        long reducedPoint = userPoint.reducePoint(amount);

        //then
        assertThat(reducedPoint).isEqualTo(userPoint.point() - amount);
    }

}