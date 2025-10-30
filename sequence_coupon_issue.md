# 쿠폰 발급 프로세스 시퀀스 다이어그램

```mermaid
sequenceDiagram
    actor User as 사용자
    participant FE as 프론트엔드
    participant CouponAPI as 쿠폰 서비스
    participant DB as 데이터베이스

    Note over User,DB: 선착순 쿠폰 발급 프로세스

    User->>FE: 1. 쿠폰 이벤트 페이지 접속
    FE->>CouponAPI: 쿠폰 목록 조회 요청
    CouponAPI->>DB: 쿠폰 정보 조회
    DB-->>CouponAPI: 쿠폰 목록 (남은 수량 포함)
    CouponAPI-->>FE: 쿠폰 목록 반환
    FE->>User: 쿠폰 목록 표시

    User->>FE: 2. 쿠폰 선택 및 발급 요청
    activate FE
    FE->>CouponAPI: 쿠폰 발급 요청 (userId, couponId)
    activate CouponAPI

    Note over CouponAPI: 동시성 제어 시작 (synchronized)

    CouponAPI->>DB: 쿠폰 정보 조회 (Lock)
    activate DB
    DB-->>CouponAPI: 쿠폰 정보 (총 수량, 발급 수량)
    deactivate DB

    alt 발급 가능 수량 있음
        CouponAPI->>DB: 중복 발급 확인 (userId, couponId)
        activate DB
        DB-->>CouponAPI: 발급 이력 조회 결과
        deactivate DB

        alt 중복 발급 아님
            CouponAPI->>DB: 트랜잭션 시작
            activate DB

            Note over CouponAPI,DB: 원자적 처리 (쿠폰 수량 차감 + 발급)

            CouponAPI->>DB: 1) 발급 수량 증가 (issuedQuantity++)
            DB-->>CouponAPI: 수량 증가 완료

            CouponAPI->>DB: 2) 사용자 쿠폰 생성 (userCouponId)
            Note over DB: status: AVAILABLE<br/>issuedAt: 현재 시간<br/>expiresAt: 발급일 + 30일
            DB-->>CouponAPI: 사용자 쿠폰 생성 완료

            CouponAPI->>DB: 트랜잭션 커밋
            DB-->>CouponAPI: 커밋 완료
            deactivate DB

            Note over CouponAPI: 동시성 제어 종료

            CouponAPI-->>FE: 발급 성공 (userCouponId, expiresAt)
            deactivate CouponAPI
            FE->>User: 발급 완료 안내 + 쿠폰 정보 표시
            deactivate FE

        else 이미 발급받음 (중복)
            Note over CouponAPI: 동시성 제어 종료
            CouponAPI-->>FE: 발급 실패 (중복 발급)
            deactivate CouponAPI
            FE->>User: "이미 발급받은 쿠폰입니다" 안내
            deactivate FE
        end

    else 수량 소진 (품절)
        Note over CouponAPI: 동시성 제어 종료
        CouponAPI-->>FE: 발급 실패 (품절)
        deactivate CouponAPI
        FE->>User: "쿠폰이 모두 소진되었습니다" 안내
        deactivate FE
    end

    opt 발급 후 쿠폰 확인
        User->>FE: 3. 내 쿠폰함 조회
        FE->>CouponAPI: 보유 쿠폰 조회 요청 (userId)
        CouponAPI->>DB: 사용자 쿠폰 목록 조회
        DB-->>CouponAPI: 쿠폰 목록 (status, expiresAt 포함)

        Note over CouponAPI: 만료된 쿠폰 자동 처리
        loop 각 쿠폰마다
            alt 만료 시간 지남 & status=AVAILABLE
                CouponAPI->>DB: 쿠폰 상태 변경 (AVAILABLE → EXPIRED)
                DB-->>CouponAPI: 상태 변경 완료
            end
        end

        CouponAPI-->>FE: 쿠폰 목록 반환
        FE->>User: 쿠폰 목록 표시 (AVAILABLE/USED/EXPIRED)
    end
```

## 주요 단계

1. **쿠폰 목록 조회**: 사용자가 발급 가능한 쿠폰 목록 확인
2. **쿠폰 선택**: 원하는 쿠폰 선택 및 발급 요청
3. **동시성 제어**: synchronized로 동시 요청 처리 (선착순 보장)
4. **수량 확인**: 발급 가능한 수량이 남아있는지 체크
5. **중복 확인**: 이미 발급받은 쿠폰인지 체크
6. **트랜잭션 처리**: 수량 차감 + 사용자 쿠폰 생성을 원자적으로 처리
7. **발급 완료**: 쿠폰 정보 반환 (만료일 포함)

## 주요 특징

### 동시성 제어 (Race Condition 방지)
```
사용자A, B가 동시에 마지막 1장 요청
→ synchronized로 순차 처리
→ A 먼저 처리 → 성공
→ B 나중 처리 → 품절 처리
```

### 원자성 (Atomicity)
- 쿠폰 수량 차감 + 사용자 쿠폰 생성
- 둘 중 하나라도 실패하면 전체 롤백

### 중복 방지
- 1인 1매 제한
- userId + couponId 조합으로 중복 체크

### 만료 처리
- 발급 시 만료일 자동 설정 (30일)
- 조회 시 만료된 쿠폰 자동 EXPIRED 처리

## 실패 케이스

1. **품절**: `totalQuantity <= issuedQuantity`
2. **중복 발급**: 이미 동일한 쿠폰 발급받음
3. **만료**: 쿠폰 조회 시 만료 시간 체크
