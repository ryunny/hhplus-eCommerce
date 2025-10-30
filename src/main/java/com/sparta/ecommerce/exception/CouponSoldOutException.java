package com.sparta.ecommerce.exception;

public class CouponSoldOutException extends BusinessException {
    public CouponSoldOutException(int issuedQuantity, int totalQuantity) {
        super(ErrorCodes.COUPON_SOLD_OUT,
            "쿠폰이 모두 소진되었습니다. (발급: " + issuedQuantity + "/" + totalQuantity + ")");
    }
}
