package io.hhplus.tdd.util;

import io.hhplus.tdd.exception.UserPointException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class UserPointValidatorTest {

    @DisplayName("id 값이 0보다 작은 경우 예외가 발생한다.")
    @Test
    void validateWithId_shouldThrowException_inputValueIsNegative() {
        // given
        long invalidId = -1L;

        // when // then
        assertThatThrownBy(() -> UserPointValidator.withId(invalidId))
                .isInstanceOf(UserPointException.class)
                .hasMessage("잘못된 id 입니다.");

    }

    @DisplayName("입력된 amount 값이 0보다 작은 경우 예외가 발생한다.")
    @Test
    void validateWithIdAndAmount_shouldThrowException_inputValueIsNegative() {
        // given
        long validId = 1L;
        long invalidAmount = 0L;

        // when // then
        assertThatThrownBy(() -> UserPointValidator.withIdAndAmount(validId, invalidAmount))
                .isInstanceOf(UserPointException.class)
                .hasMessage("포인트 사용 및 충전 금액은 0이 될 수 없습니다.");

    }
}