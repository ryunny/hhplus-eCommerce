package com.sparta.ecommerce.exception;

public class ProductNotFoundException extends BusinessException {
    public ProductNotFoundException(String productId) {
        super(ErrorCodes.PRODUCT_NOT_FOUND, "상품을 찾을 수 없습니다: " + productId);
    }
}
