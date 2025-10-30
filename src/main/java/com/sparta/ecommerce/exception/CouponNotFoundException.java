package com.sparta.ecommerce.exception;

public class CouponNotFoundException extends BusinessException {
    public CouponNotFoundException(String couponId) {
        super(ErrorCodes.COUPON_NOT_FOUND, "쿠폰을 찾을 수 없습니다: " + couponId);
    }
}
