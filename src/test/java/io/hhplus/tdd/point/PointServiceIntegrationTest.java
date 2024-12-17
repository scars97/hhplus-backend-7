package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

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

        // when
        UserPoint chargedUserPoint = pointService.chargePoint(userId, amount);

        //then
        assertThat(chargedUserPoint.id()).isEqualTo(userId);
        assertThat(chargedUserPoint.point()).isEqualTo(amount);

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

    @DisplayName("동일한 회원에 대한 충전과 사용 요청은 메서드가 실행된 순서대로 순차적으로 실행된다.")
    @Test
    void chargeAndUseRequest_withSameUser_thenExecuteSequentially() throws InterruptedException {
        // given
        long userId = 1L;
        int threadCount = 2;
        long chargeAmount = 1000L;
        long useAmount = 500L;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        LinkedBlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();

        // when
        taskQueue.put(() -> {
            try {
                try {
                    pointService.usePoint(userId, useAmount);
                } catch (IllegalArgumentException e) {
                    System.out.println("잔고 부족");
                }
            } finally {
                latch.countDown();
            }
        });
        taskQueue.put(() -> {
            try {
                pointService.chargePoint(userId, chargeAmount);
            } finally {
                latch.countDown();
            }
        });

        while (!taskQueue.isEmpty()) {
            Runnable task = taskQueue.take();
            executorService.submit(task);
        }
        latch.await();
        executorService.shutdown();

        //then
        long expectedPoint = chargeAmount - useAmount;
        assertThat(pointService.getUserPoint(userId).point()).isEqualTo(expectedPoint);
    }

}