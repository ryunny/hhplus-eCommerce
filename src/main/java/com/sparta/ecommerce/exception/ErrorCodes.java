package com.sparta.ecommerce.exception;

public class ErrorCodes {

    // 상품 관련
    public static final String PRODUCT_NOT_FOUND = "P001";
    public static final String INSUFFICIENT_STOCK = "P002";

    // 주문 관련
    public static final String INVALID_QUANTITY = "O001";
    public static final String ORDER_NOT_FOUND = "O002";

    // 결제 관련
    public static final String INSUFFICIENT_BALANCE = "PAY001";
    public static final String PAYMENT_FAILED = "PAY002";

    // 쿠폰 관련
    public static final String COUPON_NOT_FOUND = "C001";
    public static final String COUPON_SOLD_OUT = "C002";
    public static final String INVALID_COUPON = "C003";
    public static final String EXPIRED_COUPON = "C004";
    public static final String ALREADY_ISSUED = "C005";
    public static final String ALREADY_USED = "C006";
    public static final String COUPON_NOT_AVAILABLE = "C007";
}
