package com.sparta.ecommerce.exception;

public class CouponNotAvailableException extends BusinessException {
    public CouponNotAvailableException(String status) {
        super(ErrorCodes.COUPON_NOT_AVAILABLE,
            "사용 가능한 쿠폰이 아닙니다. 현재 상태: " + status);
    }
}
