# 주문/결제 프로세스 시퀀스 다이어그램

```mermaid
sequenceDiagram
    actor User as 사용자
    participant FE as 프론트엔드
    participant ProductAPI as 상품 서비스
    participant OrderAPI as 주문 서비스
    participant CouponAPI as 쿠폰 서비스
    participant PaymentAPI as 결제 서비스
    participant DB as 데이터베이스
    participant External as 외부 플랫폼

    Note over User,External: 정상 주문 흐름

    User->>FE: 1. 상품 선택
    FE->>User: 상품 정보 표시

    User->>FE: 2. 장바구니 담기
    FE->>FE: 장바구니에 추가

    User->>FE: 3. 주문하기 클릭
    FE->>ProductAPI: 재고 확인 요청
    ProductAPI->>DB: 재고 조회
    DB-->>ProductAPI: 재고 정보

    alt 재고 있음
        ProductAPI-->>FE: 재고 충분
        FE->>ProductAPI: 재고 차감 요청
        ProductAPI->>DB: 재고 차감 (Lock)
        DB-->>ProductAPI: 차감 완료
        ProductAPI-->>FE: 재고 차감 완료

        User->>FE: 4. 쿠폰 선택 (선택사항)

        opt 쿠폰 사용
            FE->>CouponAPI: 쿠폰 검증 요청
            CouponAPI->>DB: 쿠폰 유효성 확인
            DB-->>CouponAPI: 유효성 결과

            alt 쿠폰 유효
                CouponAPI-->>FE: 쿠폰 검증 성공
                FE->>CouponAPI: 쿠폰 사용 처리
                CouponAPI->>DB: 쿠폰 상태 변경 (AVAILABLE → USED)
                DB-->>CouponAPI: 상태 변경 완료
                CouponAPI-->>FE: 쿠폰 적용 완료
            else 쿠폰 무효
                CouponAPI-->>FE: 쿠폰 검증 실패
                FE->>User: 쿠폰 사용 불가 안내
            end
        end

        User->>FE: 5. 배송지 정보 입력
        FE->>OrderAPI: 주문 생성 요청 (배송지 포함)
        OrderAPI->>DB: 주문 정보 저장
        DB-->>OrderAPI: 주문 저장 완료
        OrderAPI-->>FE: 주문 ID 반환

        User->>FE: 6. 결제하기 클릭
        FE->>PaymentAPI: 결제 요청 (잔액 기반)
        PaymentAPI->>DB: 사용자 잔액 확인
        DB-->>PaymentAPI: 잔액 정보

        alt 결제 성공
            PaymentAPI->>DB: 잔액 차감
            DB-->>PaymentAPI: 차감 완료
            PaymentAPI->>OrderAPI: 결제 완료 통보
            OrderAPI->>DB: 주문 상태 변경 (PENDING → PAID)
            DB-->>OrderAPI: 상태 변경 완료
            PaymentAPI-->>FE: 결제 성공

            OrderAPI->>External: 주문 데이터 전송
            External-->>OrderAPI: 전송 완료

            FE->>User: 주문 완료 화면

        else 결제 실패
            PaymentAPI-->>FE: 결제 실패

            alt 재시도 선택
                User->>FE: 재시도 클릭
                Note over FE,PaymentAPI: 5번(배송지)부터 다시 진행
            else 취소 선택
                FE->>OrderAPI: 주문 취소 요청
                OrderAPI->>DB: 주문 상태 변경 (CANCELLED)

                opt 쿠폰 사용했다면
                    OrderAPI->>CouponAPI: 쿠폰 복구 요청
                    CouponAPI->>DB: 쿠폰 상태 변경 (USED → AVAILABLE)
                    DB-->>CouponAPI: 복구 완료
                    CouponAPI-->>OrderAPI: 쿠폰 복구 완료
                end

                OrderAPI->>ProductAPI: 재고 복구 요청
                ProductAPI->>DB: 재고 증가
                DB-->>ProductAPI: 복구 완료
                ProductAPI-->>OrderAPI: 재고 복구 완료

                OrderAPI-->>FE: 주문 취소 완료
                FE->>User: 상품 선택 화면으로 복귀
            end
        end

    else 재고 없음
        ProductAPI-->>FE: 재고 부족
        FE->>User: 품절 안내
    end
```

## 주요 단계

1. **상품 선택 및 장바구니**: 사용자가 상품을 선택하고 장바구니에 담음
2. **재고 확인 및 차감**: 주문 시 재고를 확인하고 즉시 차감 (동시성 제어)
3. **쿠폰 검증 및 적용**: 선택 시 쿠폰 유효성 검증 후 사용 처리
4. **배송지 입력**: 수령인, 주소, 연락처 입력
5. **결제 처리**: 잔액 기반 결제 진행
6. **주문 완료**: 결제 성공 시 외부 플랫폼에 데이터 전송
7. **롤백 처리**: 결제 실패 시 재고/쿠폰 복구

## 주요 특징

- **동시성 제어**: 재고 차감 시 Lock 사용
- **트랜잭션**: 각 단계별 원자성 보장
- **롤백**: 실패 시 재고/쿠폰 자동 복구
- **재시도**: 결제 실패 시 사용자가 재시도 선택 가능
