package io.hhplus.tdd.util;

import io.hhplus.tdd.UserPointException;

public class UserPointValidator {

    public static void withId(long id) {
        if (id <= 0) {
            throw new UserPointException("잘못된 id 입니다.");
        }
    }

    public static void withIdAndAmount(long id, long amount) {
        if (id <= 0) {
            throw new UserPointException("잘못된 id 입니다.");
        }

        if (amount <= 0) {
            throw new UserPointException("포인트 사용 및 충전 금액은 0이 될 수 없습니다.");
        }
    }

}
