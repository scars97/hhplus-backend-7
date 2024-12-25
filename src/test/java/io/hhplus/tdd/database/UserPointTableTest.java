package io.hhplus.tdd.database;

import io.hhplus.tdd.point.UserPoint;
import org.junit.jupiter.api.*;

import static org.assertj.core.api.Assertions.*;

class UserPointTableTest {

    private UserPointTable userPointTable;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
    }

    @DisplayName("등록되지 않은 회원 id로 포인트 조회 시, 포인트가 0으로 반환된다.")
    @Test
    void unregisteredUserThenPointIsZero() {
        // given
        long emptyId = 1L;

        // when
        UserPoint userPoint = userPointTable.selectById(emptyId);

        // then
        assertThat(userPoint.id()).isEqualTo(emptyId);
        assertThat(userPoint.point()).isZero();
    }
}