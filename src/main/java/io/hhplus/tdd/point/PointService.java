package io.hhplus.tdd.point;

import io.hhplus.tdd.UserPointException;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PointService {

    private static final long MIN_CHARGE_AMOUNT = 1000L;

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;
    private final ConcurrentHashMap<Long, Lock> locks = new ConcurrentHashMap<>();

    private Lock getLock(long userId) {
        return locks.computeIfAbsent(userId, id -> new ReentrantLock(true));
    }

    @Autowired
    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public UserPoint getUserPoint(long id) {
        return userPointTable.selectById(id);
    }

    public List<PointHistory> getPointHistories(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }

    public UserPoint chargePoint(long id, long amount) {
        Lock lock = getLock(id);
        lock.lock();
        try {
            if (amount < MIN_CHARGE_AMOUNT) {
                throw new UserPointException("포인트 충전은 1,000원 이상부터 가능합니다.");
            }

            UserPoint userPoint = userPointTable.selectById(id);

            pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());

            return userPointTable.insertOrUpdate(id, userPoint.addPoint(amount));
        } finally {
            lock.unlock();
        }
    }

    public UserPoint usePoint(long id, long amount) {
        Lock lock = getLock(id);
        lock.lock();
        try {
            UserPoint userPoint = userPointTable.selectById(id);

            if (userPoint.point() < amount) {
                throw new UserPointException("잔고 부족");
            }

            pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());

            return userPointTable.insertOrUpdate(id, userPoint.reducePoint(amount));
        } finally {
            lock.unlock();
        }
    }
}
