package io.hhplus.tdd.database;

import io.hhplus.tdd.point.PointHistory;
import io.hhplus.tdd.point.TransactionType;
import org.junit.jupiter.api.*;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class PointHistoryTableTest {

    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        pointHistoryTable = new PointHistoryTable();
    }

    @DisplayName("포인트 내역 저장 시나리오")
    @TestFactory
    Collection<DynamicTest> pointHistoryDynamicTest() {
        //given
        long userId = 1L;
        TransactionType type = TransactionType.CHARGE;
        long updateMillis = System.currentTimeMillis();
        long expectCursor = 1L;

        return List.of(
            DynamicTest.dynamicTest("포인트 내역이 정상적으로 저장된다.", () -> {
                // given
                long amount = 1000L;

                // when
                PointHistory pointHistory = pointHistoryTable.insert(userId, amount, type, updateMillis);

                // then
                assertThat(pointHistory)
                        .extracting("id", "userId", "amount", "type", "updateMillis")
                        .containsExactly(expectCursor, userId, amount, type, updateMillis);
            }),
            DynamicTest.dynamicTest("저장 시, 포인트 내역 순번이 1씩 늘어난다.", () -> {
                // given
                long amount = 500L;
                long nextCursor = expectCursor + 1L;

                // when
                PointHistory pointHistory = pointHistoryTable.insert(userId, amount, type, updateMillis);

                // then
                assertThat(pointHistory)
                        .extracting("id", "userId", "amount", "type", "updateMillis")
                        .containsExactly(nextCursor, userId, amount, type, updateMillis);
            })
        );
    }
}