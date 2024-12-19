### **동시성 문제는 어떻게 발생하게 되는 걸까?**

---

공유 자원에 대해 분리된 연산을 수행하는 과정에서 발생한다.

Thread A와 Thread B 가 거의 동시에 한 회원 ID로 포인트 충전을 요청한 경우,

A,B Thread 모두 동일한 데이터를 읽어 그 데이터를 기반으로 작업을 수행하고 저장할 때,<br> 서로의 작업이 덮어쓰여 데이터 불일치 문제가 발생한다.

### **동시성 제어를 하지 않았을 때 발생할 수 있는 문제**

---

- Dirty Read : 다른 트랜잭션에서 완료되지 않은 변경 사항을 읽어 발생하는 문제. 한 스레드가 데이터를 수정하는 중에 다른 스레드가 해당 데이터를 앍어 잘못된 상태로 작업이 수행될 수 있다.
- Lost Update : n개의 스레드가 동시에 데이터를 수정하고 저장할 때, 한 스레드의 업데이트가 다른 스레드의 업데이트에 의해 덮어쓰여져 사라진다.

### **동시성을 제어할 수 있는 방법**

---

현 과제에서는 분산환경을 고려하지 않으며, Map의 활용한 인메모리 DB 방식을 사용 중이다.<br>
그렇다면 애플리케이션 레벨에서 동시성 제어를 해볼 수 있을 것 같다.
자바에서 지원하는 동시성 제어 방식은 어떤 것이 있을까

**1. Lock 기반 동기화 -** synchronized, ReentrantLock

**synchronized (암묵적 락)**

- 자바의 모든 객체는 모니터 락을 가지고 있는데, synchronized 키워드를 통해 암묵적으로 사용된다.
- 특정 객체의 모니터 락을 획득한 스레드만 해당 객체의 synchronized 메서드나 블록을 실행할 수 있다.
- 다른 스레드는 모니터 락이 해제될 떄까지 대기해야 한다.

**ReentrantLock (명시적 락)**

- synchronized 보다 더 세밀하게 락을 제어할 수 있다.
- lock(), unlock() 을 명시적으로 호출해야 락을 사용할 수 있기 때문에 휴먼에러가 발생할 수 있다.
- synchronized 의 경우 여러 스레드가 동시 요청 했을 때 스레드의 순서를 보장할 수 없다.<br>
  ReenrantLock 또한 기본적으로 순서를 보장하지 않지만 공정락(fairness) 모드로 설정하면 대기 중인 스레드에게 순차적으로 락이 할당된다.
- ReenrantLock 의 공정 모드는 락 내부에서 큐를 사용하여 대기 중인 스레드의 순서를 보장하는데, <br> 락이 해제되면 대기 큐의 맨 앞에 있는 스레드가 우선적으로 락을 획득한다.
- 대기 큐를 관리하기 때문에 상황에 따라 성능에 좋지 않을 수 있고,
  빠르게 처리되어야 하는 스레드가 락을 요청하면 바로 락을 획득하지 못하고 대기 큐에 들어가야 한다.

**2. Lock Free**

**CAS 알고리즘 (Compare-And-Swap)**

- 연산에서 기대하는 값과 저장된 값을 비교해서 두 개가 일치할 때만 값을 수정한다.
- 락을 사용하지 않아 락 경합이 일어나지 않는다는 장점이 있는 대신,
  루프를 돌기 때문에 자원이 많이 소모될 수 있고, 구현이 복잡하다.

### **내가 사용한 방법**

---

동시성을 허용하면서 동시성을 제어해야 한다.

- A의 충전, B의 사용, C의 충전 및 사용 → 각 회원마다 동시에 실행되어야 한다.
- A의 충전, 사용, 사용 → 순차적으로 실행되어야 한다.

**1. synchronized**

```jsx
// 1.
public synchronized UserPoint chargePoint(long id, long amount) {
    if (amount < MIN_CHARGE_AMOUNT) {
        throw new UserPointException("포인트 충전은 1,000원 이상부터 가능합니다.");
    }
    UserPoint userPoint = userPointTable.selectById(id);
    PointHistory pointHistory = pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
    long resultPoint = userPoint.addPoint(pointHistory.amount());
    return userPointTable.insertOrUpdate(id, resultPoint);
}

// 2.
public UserPoint chargePoint(long id, long amount) {
    synchronized (userPointTable) {
            if (amount < MIN_CHARGE_AMOUNT) {
            throw new UserPointException("포인트 충전은 1,000원 이상부터 가능합니다.");
        }
        UserPoint userPoint = userPointTable.selectById(id);
        PointHistory pointHistory = pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
        long resultPoint = userPoint.addPoint(pointHistory.amount());
        return userPointTable.insertOrUpdate(id, resultPoint);
    }
}
```

같은 회원이 동시에 여러 요청을 한 경우 synchronized로 인해 동시성 문제를 방지한다.<br>
하지만 각각 다른 회원의 요청에도 동시성이 제어되어 불필요한 대기가 발생한다.

동시성 제어를 위해 모든 접근을 차단하게 되므로 성능에 좋지 않다. 그리고 작업의 순서를 보장할 수 없다.

---

**2. ConcurrentHashMap & ReentrantLock - fairness**

ConcurrentHashMap 은 내부적으로 CAS 를 사용하여 병렬성을 제공하지만 특정 버킷에만 락을 걸어 동시성을 제어한다.

Map 에 회원 Id가 없는 경우 빈 버킷에 CAS를 사용하여 새로운 노드를 삽입하고,<br>
이미 존재하는 경우 해당 버킷의 노드를 synchronized를 사용해 다른 스레드가 접근하지 못하도록 막는다.

이러한 구조로 덕분에 여러 회원의 요청에 동시적으로 작업을 실행할 수 있다.

동일 회원 요청에 대한 동시성 제어는 ReentrantLock을 사용했다.<br>
ConcurrentHashMap의 value로 ReentrantLock을 사용하여 회원 ID 별로 동기화를 보장한다.
작업의 순서를 보장하기 위해 ReentrantLock 공정 모드로 구현했다.

```jsx
private final ConcurrentHashMap<Long, Lock> locks = new ConcurrentHashMap<>();

private Lock getLock(long userId) {
    return locks.computeIfAbsent(userId, id -> new ReentrantLock(true));
}

public UserPoint chargePoint(long id, long amount) {
    Lock lock = getLock(id);
    lock.lock();
    try {
        if (amount < MIN_CHARGE_AMOUNT) {
            throw new UserPointException("포인트 충전은 1,000원 이상부터 가능합니다.");
        }
        UserPoint userPoint = userPointTable.selectById(id);
        PointHistory pointHistory = pointHistoryTable.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());
        long resultPoint = userPoint.addPoint(pointHistory.amount());
        return userPointTable.insertOrUpdate(id, resultPoint);
    } finally {
        lock.unlock();
    }
}
```

---

### **CompletableFuture vs. ExecutorService + CountDownLatch**

동시성 테스트는 어떤 방법을 사용해볼 수 있을까

**CompletableFuture**

- 비동기 작업을 선언적으로 작성할 수 있다.
- 코드가 더 간결하고, 명시적으로 CountDownLatch를 관리할 필요가 없다.
- ForkJoinPool(공유 스레드 풀)을 사용하며, 스레드 재사용이 효율적이다.
- 작업이 완료되면 자동으로 풀에 반환되므로 스레드 자원 관리에 좋다.
- 작업을 더 간결하게 작성하고, 스레드 관리를 자동화하고 싶을 때 선택할 수 있다.

```jsx
@DisplayName("동일한 회원에 대한 충전과 사용 요청에 의한 작업이 순차적으로 실행된다.")
@Test
void chargeAndUseRequest_withSameUser_thenExecuteSequentially() {
    // given
    long userId = 1L;
    long chargeAmount = 1000L;
    long useAmount = 500L;

    // when
    CompletableFuture.allOf(
        CompletableFuture.runAsync(() -> pointService.chargePoint(userId, chargeAmount)),
        CompletableFuture.runAsync(() -> pointService.usePoint(userId, useAmount)),
        CompletableFuture.runAsync(() -> pointService.usePoint(userId, useAmount)),
        CompletableFuture.runAsync(() -> pointService.chargePoint(userId, chargeAmount)),
        CompletableFuture.runAsync(() -> pointService.chargePoint(userId, chargeAmount))
    ).join();

    assertThat(pointService.getUserPoint(userId).point()).isEqualTo(2000L);
}
```

**ExecutorService + CountDownLatch**

- 커스텀 스레드 풀을 생성하여 사용할 수 있지만, 스레드 수를 명시적으로 관리해야 한다.
- 동시성을 직접 제어해야 하며, 모든 작업이 완료될 때까지 명시적으로 대기해야 한다.
- 더 세밀한 동시성 제어 테스트가 필요한 경우 선택할 수 있다.

```jsx
@DisplayName("동일한 회원에 대한 충전과 사용 요청에 의한 작업이 순차적으로 실행된다.")
@Test
void chargeAndUseRequest_withSameUser_thenExecuteSequentially() { throws InterruptedException {
    // given
    long userId = 1L;
    long chargeAmount = 1000L;
    long useAmount = 500L;
    int threadCount = 10;

    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CountDownLatch latch = new CountDownLatch(threadCount);

    // when
    for (int i = 0; i < threadCount / 2; i++) {
        executorService.submit(() -> {
            try {
                pointService.chargePoint(userId, chargeAmount);
            } finally {
                latch.countDown();
            }
        });

        executorService.submit(() -> {
            try {
                try {
                    pointService.usePoint(userId, useAmount);
                } catch (IllegalArgumentException e) {
                    // 잔고 부족 예외 무시 (정상적인 시나리오)
                }
            } finally {
                latch.countDown();
            }
        });
    }
    latch.await();
    executorService.shutdown();

    //then
    long expectedPoint = threadCount / 2 * (chargeAmount - useAmount);
    assertThat(pointService.getUserPoint(userId).point()).isEqualTo(expectedPoint);
}
```