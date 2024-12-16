## 1주차 - TDD로 개발하기

---

### Intro
Q. 구현하고자 하는 것이 무엇인가?
- 회원의 포인트 사용, 충전 기능 및 포인트 내역 조회

Q. 무엇을 고려해야 할까?
- 같은 회원의 포인트 사용/충전 요청이 동시에 들어온다면 순서대로 제어될 수 있도록해야 한다.
- 잔고가 부족한 경우, 포인트 사용에 실패하고 적절한 예외를 발생시켜야 한다.
- 충전은 최소 1,000원 이상부터 시작해야 한다.

Q. 어떻게 구현할 것인가?
- red - green - refactoring
    - red : 비즈니스 로직 구현 없이 테스트 케이스를 토대로, 적혀있는 글을 코드로 표현한다.
    - green : 하드코딩을 통해 테스트를 성공시킨다.
    - refactoring : 성공한 테스트를 토대로 비즈니스 로직을 구현한다.
---
### Test Case
- 포인트 충전
    - 하나의 트랜잭션 안에서 포인트 충전과 해당 충전에 대한 포인트 내역이 저장된다.
    - 입력된 포인트 값이 1000 미만인 경우, 포인트 충전에 실패한다.
- 포인트 사용
    - 보유 포인트가 무조건 있어야 포인트 사용을 할 수 있다.
    - 보유한 포인트보다 입력한 포인트가 더 큰 경우, 포인트 사용에 실패한다.
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
- synchronized 말고 적용할 수 있는 게 없는 것 같다.
  - DB 벤더를 사용하고 있지 않고, Map 구조도 변경할 수 없다.
  - Atomic Type은 어떻게 구현ㅎ..ㅐ..
</div>
</details>