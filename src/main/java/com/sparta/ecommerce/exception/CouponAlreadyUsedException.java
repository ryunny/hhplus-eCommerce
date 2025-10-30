package com.sparta.ecommerce.exception;

public class CouponAlreadyUsedException extends BusinessException {
    public CouponAlreadyUsedException() {
        super(ErrorCodes.ALREADY_USED, "이미 사용된 쿠폰입니다.");
    }
}
