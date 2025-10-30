package com.sparta.ecommerce.exception;

public class InsufficientStockException extends BusinessException {
    public InsufficientStockException(int currentStock, int requestedQuantity) {
        super(ErrorCodes.INSUFFICIENT_STOCK,
            "재고가 부족합니다. 현재 재고: " + currentStock + ", 요청 수량: " + requestedQuantity);
    }
}
