package com.sparta.ecommerce.exception;

public class CouponExpiredException extends BusinessException {
    public CouponExpiredException() {
        super(ErrorCodes.EXPIRED_COUPON, "만료된 쿠폰입니다.");
    }
}
