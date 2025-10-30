package com.sparta.ecommerce.exception;

public class CouponAlreadyIssuedException extends BusinessException {
    public CouponAlreadyIssuedException() {
        super(ErrorCodes.ALREADY_ISSUED, "이미 발급받은 쿠폰입니다.");
    }
}
