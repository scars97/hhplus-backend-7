## 1주차 - TDD로 개발하기

---

### Intro
Q. 구현하고자 하는 것이 무엇인가?
- 회원의 포인트 사용, 충전 기능 및 포인트 내역 조회

Q. 무엇을 고려해야 할까?
- 특정 유저의 작업(사용/충전)은 순차적으로 실행되어야 하지만 각 유저에 대한 작업은 동시에 실행될 수 있다.
- 잔고가 부족한 경우, 포인트 사용에 실패해야 한다.
- 충전은 최소 1,000원 이상부터 시작해야 한다.
- 포인트 사용/충전 요청에 입력된 포인트 값이 0 이하인 경우 실패해야 한다.
- 입력된 회원 id가 0 이하인 경우 실패해야 한다.

Q. 어떻게 구현할 것인가?
- red - green - refactoring
    - red : 비즈니스 로직 구현 없이 테스트 케이스를 토대로, 적혀있는 글을 코드로 표현한다.
    - green : 하드코딩을 통해 테스트를 성공시킨다.
    - refactoring : 성공한 테스트를 토대로 비즈니스 로직을 구현한다.
---
### 고민
<details>
<summary>12/15</summary>
<div markdown="1">

Q. 포인트 사용/충전과 포인트 내역 저장은 하나의 트랜잭션에서 실행되어야 한다. 예외가 발생한 경우 어떻게 롤백할 수 있을까?
- 과제에서는 DB 기술을 사용하지 않고 자바로 구현되어 있다.
- 프레임워크에서 제공하는 @Transactional을 사용할 수 없는 상황이다.

Q. 단위 테스트와 통합 테스트 경계
- **단위 테스트와 통합 테스트의 기준**을 잘 모르겠음.
  - 단위 테스트 - 작은 단위의 메서드, 기능
  - 통합 테스트 - 관련 기능에 대한 레이어 테스트
- 내가 생각하는 단위 테스트란 특정 메서드가 외부 의존성이 어떤 상태든 상관없이 순수하게 동작해야 하는 것.
  - 그렇기 때문에 외부 의존성을 mocking 해야 한다고 생각한다.
  - mocking 하게 되면 내가 외부 의존성 코드를 제어할 수 있으니까.

Q. 비즈니스 로직이 잘못되어도 Mock으로 인해 정해진 값을 사용하기 때문에 테스트는 성공한다. 이걸 테스트라 할 수 있을까?
```java
// 비즈니스 로직이 제대로 동작했는지 알 수 없다.
@DisplayName("망한 테스트")
@Test
void shit_test() {
    // given
    long userId = 1L;
    long amount = 1000L;

    UserPoint mockUserPoint = new UserPoint(userId, 5000L, System.currentTimeMillis());
    PointService pointService = mock(PointService.class);
    when(pointService.chargePoint(userId, amount)).thenReturn(new UserPoint(userId, 6000L, System.currentTimeMillis()));

    // when
    UserPoint chargedPoint = pointService.chargePoint(userId, amount);

    // then
    assertThat(chargedPoint.id()).isEqualTo(userId);
    assertThat(chargedPoint.point()).isEqualTo(mockUserPoint.point() + amount);
}
```
- Mock을 사용해서 테스트 한다면, 어떻게 작성해야 비즈니스 로직을 검증하는 진짜 테스트가 될까?

Q. 포인트 사용 및 충전 후, 해당 건에 대한 포인트 내역이 저장되어야 하는데, 테스트를 작성할 때 포인트 내역 저장을 어떻게 검증해야할까?
- 총 2개의 주제가 한 메서드에서 테스트가 되는건데 그렇게 작성하는 게 맞나…?
- 한 테스트 메서드 안에서 사용 및 충전 검증 로직 뒤에, 포인트 내역 조회 메서드를 통해 해당 건이 저장되었는지 테스트한다. → DynamicTest를 사용해보면 될까?
- 포인트 내역 조회 테스트 메서드를 만들어 호출하도록 한다. → 기존 포인트 내역 조회 테스트와 호환되게 만들어야 한다.  
</div>
</details>

<details>
<summary>12/16</summary>
<div markdown="1">

Q. 과제에서는 어떤 상황에서 동시성이 발생할까?
- 충전 요청과 사용 요청이 동시에 들어오는 경우
- 동일한 사용자가 n번의 포인트 사용 요청을 동시에 보냈을 때 -> Race Condition
- A 사용자의 포인트 충전이 진행되는 동안, 관리자가 A 사용자의 포인트 또는 내역을 조회할 때
  - 조회된 값이 충전 이전의 값일 수 있다. -> Dirty Read

Q. 동시성 제어 구현과 테스트는 어떻게 해야 할까?
- 구현
  - 메서드에 synchronized 키워드를 사용해서 한 요청에 대한 응답이 끝날 때까지 블로킹한다. → 요청이 많아지면 성능에 좋지 않다.
- 테스트
  - ExecutorService로 여러 스레드 생성, CountDownLatch로 모든 스레드가 동시에 작업을 시작하도록 조정.
  - Mock 을 사용해서 동시성 테스트를 해도 될까?
    - 동시성 테스트는 실제 환경에서의 데이터 정합성과 스레드 안정성을 검증하는 것이 목적이다.
    - Mock을 사용한 이유는 외부 의존성을 대체하여 순수하게 테스트 대상 코드가 동작하는지 검증하기 위함이다.
    - 목적 자체가 다르고, Mock을 사용한 동시성 테스트는 실제 데이터 정합성을 정확하게 테스트할 수 없다.
      - Mock 객체는 스레드 안전하지 않기 때문이다.

Q. 동시성 제어 방법은 어떤 것이 있고, 현 상황에서 적용할 수 있는 방법은 뭘까?
- 제어 방법
  - 동기화 : 메서드에 synchronized를 사용하여 제어.
  - DB Lock : 낙관적/비관적 락
  - Atomic Type
  - ConCurrentHashMap
</div>
</details>

<details>
<summary>12/17</summary>
<div markdown="1">

Q. PointHistoryTable의 insert()는 왜 저장 시간을 파라미터로 받을까?
- UserPointTable의  insertOrUpdate()는 메서드 내부에서 시간값을 결정한다.
- 동적 값에 대한 테스트를 고려하라는 의도일까?
- PointService의 chargePoint()에서 똑같이 외부에서 파라미터를 전달받도록 구현해봤다.

```java
public UserPoint chargePoint(long id, long amount, long updateMillis) {
	// ...
	PointHistory pointHistory = 
		pointHistoryTable.insert(id, amount, TransactionType.CHARGE, updateMills);
	/*PointHistory pointHistory = pointHistoryTable
			.insert(id, amount, TransactionType.CHARGE, System.currentTimeMillis());*/
	//...
}
```
- Service에서도 굳이 시간 값을 외부에서 전달받아야하는지 의문이 듦.
- 시간 값이 결정되는 시점은 포인트 내역 저장 메서드가 호출될 때라고 생각.
- 비즈니스 로직이 동작하는 것에는 파라미터로 전달 받든 직접 넣든 상관은 없음.
- 하지만 mock을 사용한 단위 테스트에서 문제 발생
- 시간 주입을 위한 인터페이스를 만들어서 고정된 값으로 테스트해보는 방법이 있지만 Table 클래스의 수정이 필요하다.
- Mocito의 ArgumentCaptor를 사용해서 메서드 호출 시점에서 시간 값을 캡처하고 검증하는 방식으로 테스트를 작성했다.
- 비즈니스 로직은 정상적으로 동작하지만 테스트가 실패해서 비즈니스 로직을 수정한다…?
- 충전 기능에 대한 테스트인데 포인트 내역 저장에서 문제가 발생한다.

Q. 포인트 내역 저장 시간 테스트
- 포인트 충전과 사용 시, 해당 건에 대한 내역이 저장되는데, 이때 저장시간을 DB 내부에서 결정하는 것이 아니라 외부에서 시간을 전달받는다.
- 외부 의존 객체를 mocking 하게 되면 지정한 응답으로 덮여쓰여야 하는데, 시간 데이터는 다른 값으로 입력되는 문제가 발생했다.
- 비즈니스 로직에서는 시간 파라미터를 변수로 지정한게 아니라 System.currentTimeMillis() 를 직접 파라미터로 전달한다.
- 시간 불일치로 테스트가 실패하는데, 메서드 호출 시 결정되는 시간 값을 캡쳐하여 검증하는 방식으로 해결했다. → ArgumentCaptor

Q. synchronized
- synchronized lock 범위에 따라 동기화되는 방식이 다르다.
- 정적 메서드 (Class Lock) : 클래스의 Class 객체에 대한 락이 걸린다.
  → 여러 스레드가 해당 클래스에 대한 인스턴스를 각각 가지고 있다해도, 한 스레드가 점유하고 있으면 동작이 끝날 때까지 접근하지 못한다.
  → 클래스의 정적 메서드에 접근하려는 모든 스레드는 하나의 Class 객체에 대해 경쟁하게 된다.
- 일반 메서드 (Instance Lock) : 클래스의 인스턴스에 대한 락이 걸린다.
  → 스레드마다 서로 다른 인스턴스를 사용하면, 각 인스턴스는 독립된 락을 가지므로 동일한 메서드에 동시에 접근할 수 있다.
  → 반대로 스레드마다 동일한 인스턴스를 사용하면, 한 번에 하나의 스레드만 메서드를 실행 할 수 있다.
- 스프링은 싱글톤 방식을 사용하기 때문에 모든 스레드가 동일한 인스턴스를 공유하게 된다.

```java
// 포인트 충전과 사용 요청이 동시에 들어오는 경우
ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
CountDownLatch latch = new CountDownLatch(threadCount);
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

            }
        } finally {
            latch.countDown();
        }
    });
}
latch.await();
executorService.shutdown();

// 비즈니스 로직
public synchronized UserPoint chargePoint(...) {
	//...
}
public synchronized UserPoint usePoint(...) {
	//...
}
```
- 포인트 충전과 사용 요청이 동시에 실행되는 상황을 가정했지만 동시 실행이 아닌 순차적으로 실행된다.(순서x)
- 모든 스레드가 동일한 pointService 인스턴스에 접근하게 되고, 충전과 사용 메서드가 **순차적으로 실행**된다.
- synchronized 를 사용한다면 메서드 레벨이 아닌 특정 블록에 사용하여 락 범위를 줄이는 것이 좋다.
  - 어떤 행위(충전, 사용)에 대해서는 순차적으로 실행될 수 있지만, 다른 유저의 요청이 들어온다면 이전 작업이 끝날 때까지 대기해야 한다.
    → id = 1  회원의 작업이 끝나야만 id = 2 회원의 작업이 실행될 수 있다.

```java
UserPointTable userPointTable;
public UserPoint chargePoint(long id, long amount) {
    synchronized (userPointTable) { // 특정 유저에 대한 동기화
        // ...
    }
}
```
synchronized는 순차적인 실행을 보장하지만, **작업의 순서는 보장하지 않는다.**

Q. Mock & Stub

Mock
- 구체적인 구현이 필요하다면 mock 을 사용한다.
- 입력과 상관없이 어떤 행동을 할 지에 초점을 맞춘다.

```java
when(userRepository.getUser(id)).thenReturn(new User(1L, "username"));
```

Stub
- 객체의 행동에 상관없이 결과만 필요할 때 test용으로 따로 정의해서 구현할 수 있다.
- 재사용의 장점이 있다.

```java
// test 디렉토리에만 존재
class FakeUserRepository implements UserRepository {
		public User getUser(Long id) {
				return new User(1L, "username");
		}
}
```

Q. synchronized vs ConcurrentHashMap/ReentrantRock

- A의 충전, B의 사용, C의 충전 및 사용 → 각 회원마다 동시에 실행되어야 한다.
- A의 충전, 사용, 사용 → 순차적으로 실행되어야 한다.
- syn의 경우 동시성을 제어하기 때문에 두 번째 요구에는 적합하지만 동시성을 필요로 하는 첫 번째 요구에는 적합하지 않다.
- ConcurrentHashMap 은 특정 버킷에 대한 Lock을 사용한다. 이 말은 각각의 회원마다 Lock을 가질 수 있다는 것이다. → ABC 회원의 요청을 동시에 실행시킬 수 있다.
- ConcurrentHashMap에서 사용할 Lock 메커니즘을 ReentrantLock으로 설정한다면 동일한 회원의 요청에는 순차적으로 실행되게 한다.
- ReentrantLock은 하나의 **공유된 락**을 기반으로 동작한다.
- 스레드가 lock을 호출하면, 다른 스레드는 락이 해제될 때까지 대기하게 된다.
- 이 때문에 하나의 스레드가 작업을 마칠 때까지 **다른 스레드가 실행될 수 없으므로**, 작업이 순차적으로 진행되는 것처럼 보인다.
- 여기서 순차적으로 진행되는 것처럼 보인다라는 말은 **스레드가 실제로 순서대로 실행될 수도 있지만, 그 순서를 절대적으로 보장하지는 않는다**는 의미이다.

Q. 순서는 어떻게 보장할 수 있을까
- 순서 보장을 위해 BlockingQueue를 사용했다.
- 큐에 저장된 작업을 꺼내는 순서는 맞을지 몰라도 락을 획득하는 스레드가 임의로 실행되기 때문에 BlockingQueue의 순서와 실제 실행 순서가 일치하지 않게 된다.
- ReentrantLock의 공정락을 사용하여 순서를 보장할 수 있는데, 락을 요청한 시점 순서대로 별도의 큐를 두고 순차적으로 실행되게 한다. → fairness
</div>
</details>

<details>
<summary>12/18</summary>
<div markdown="1">

Q. Controller 테스트
- 통합 테스트로 진행해줘야 하는걸까?
  → 외부에서부터 외부로까지 요청과 응답이 내가 원하는대로 이루어지는지 확인하려면 통합 테스트가 맞는 것 같다.
  그렇다면 controller를 단위테스트로 작성하는 사례도 있을까?
- 지금 막힌 부분이 회원 포인트 내역 목록을 조회하는 테스트인데
  데이터가 있어야 조회를 할 수 있다. controller 테스트인데 데이터를 만드는 로직을 넣어야할까?
1. 포인트 사용/충전 과 내역 조회를 다이나믹 테스트로 묶어볼 수 있다.
   → 사용과 충전 테스트는 따로 작성해야하기 때문에 내역 조회에 대한 코드가 중복된다.
2. 사용/충전 테스트 마지막에 내역 조회 테스트 메서드를 삽입해볼 수있다.
</div>
</details>