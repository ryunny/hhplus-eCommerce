package com.sparta.ecommerce.controller;

import com.sparta.ecommerce.exception.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@Tag(name = "쿠폰 관리", description = "쿠폰 발급 및 사용 관리 API")
public class CouponController {

    // 쿠폰 마스터 데이터
    private static final Map<String, Map<String, Object>> COUPONS = new ConcurrentHashMap<>();

    // 사용자별 발급된 쿠폰 (userId -> List of userCoupon)
    private static final Map<String, List<Map<String, Object>>> USER_COUPONS = new ConcurrentHashMap<>();

    static {
        // 초기 쿠폰 데이터
        Map<String, Object> c1 = new HashMap<>();
        c1.put("id", "C001");
        c1.put("name", "10% 할인 쿠폰");
        c1.put("discountRate", 10);
        c1.put("totalQuantity", 100);
        c1.put("issuedQuantity", 2);  // 이미 2명 발급
        COUPONS.put("C001", c1);

        Map<String, Object> c2 = new HashMap<>();
        c2.put("id", "C002");
        c2.put("name", "5000원 할인 쿠폰");
        c2.put("discountAmount", 5000);
        c2.put("totalQuantity", 50);
        c2.put("issuedQuantity", 1);  // 이미 1명 발급
        COUPONS.put("C002", c2);

        Map<String, Object> c3 = new HashMap<>();
        c3.put("id", "C003");
        c3.put("name", "20% 할인 쿠폰");
        c3.put("discountRate", 20);
        c3.put("totalQuantity", 30);
        c3.put("issuedQuantity", 0);  // 아직 발급 안 됨
        COUPONS.put("C003", c3);

        // 초기 사용자 쿠폰 데이터 (테스트용)
        // USER001의 쿠폰들
        List<Map<String, Object>> user001Coupons = new ArrayList<>();

        Map<String, Object> uc1 = new HashMap<>();
        uc1.put("id", "UC-001");
        uc1.put("userId", "USER001");
        uc1.put("couponId", "C001");
        uc1.put("status", "AVAILABLE");
        uc1.put("issuedAt", "2024-10-01T10:00:00");
        uc1.put("expiresAt", "2024-11-01T10:00:00");
        user001Coupons.add(uc1);

        Map<String, Object> uc2 = new HashMap<>();
        uc2.put("id", "UC-002");
        uc2.put("userId", "USER001");
        uc2.put("couponId", "C002");
        uc2.put("status", "USED");
        uc2.put("issuedAt", "2024-10-01T10:00:00");
        uc2.put("expiresAt", "2024-11-01T10:00:00");
        uc2.put("usedAt", "2024-10-15T14:30:00");
        user001Coupons.add(uc2);

        USER_COUPONS.put("USER001", user001Coupons);

        // USER002의 쿠폰들
        List<Map<String, Object>> user002Coupons = new ArrayList<>();

        Map<String, Object> uc3 = new HashMap<>();
        uc3.put("id", "UC-003");
        uc3.put("userId", "USER002");
        uc3.put("couponId", "C001");
        uc3.put("status", "AVAILABLE");
        uc3.put("issuedAt", "2024-10-02T11:00:00");
        uc3.put("expiresAt", "2024-11-02T11:00:00");
        user002Coupons.add(uc3);

        USER_COUPONS.put("USER002", user002Coupons);
    }

    /**
     * GET /api/coupons
     * 쿠폰 목록 조회
     */
    @GetMapping("/coupons")
    public Map<String, Object> getCoupons() {
        List<Map<String, Object>> couponList = new ArrayList<>(COUPONS.values());
        return Map.of("coupons", couponList);
    }

    /**
     * POST /api/coupons/{couponId}/issue
     * 쿠폰 발급 (선착순)
     */
    @Operation(
        summary = "쿠폰 발급",
        description = "선착순으로 쿠폰을 발급합니다 (1인 1매, 동시성 제어)",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "쿠폰 발급 요청",
            content = @io.swagger.v3.oas.annotations.media.Content(
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\"userId\": \"USER001\"}"
                )
            )
        )
    )
    @PostMapping("/coupons/{couponId}/issue")
    public synchronized Map<String, Object> issueCoupon(
            @Parameter(description = "쿠폰 ID") @PathVariable String couponId,
            @RequestBody Map<String, Object> request) {

        String userId = (String) request.get("userId");

        // 쿠폰 존재 확인
        Map<String, Object> coupon = COUPONS.get(couponId);
        if (coupon == null) {
            throw new CouponNotFoundException(couponId);
        }

        // 발급 가능 수량 확인
        int totalQuantity = (int) coupon.get("totalQuantity");
        int issuedQuantity = (int) coupon.get("issuedQuantity");

        if (issuedQuantity >= totalQuantity) {
            throw new CouponSoldOutException(issuedQuantity, totalQuantity);
        }

        // 중복 발급 확인
        List<Map<String, Object>> userCouponList = USER_COUPONS.getOrDefault(userId, new ArrayList<>());
        boolean alreadyIssued = userCouponList.stream()
                .anyMatch(uc -> couponId.equals(uc.get("couponId")));

        if (alreadyIssued) {
            throw new CouponAlreadyIssuedException();
        }

        // 쿠폰 발급 (발급 후 30일 유효)
        String userCouponId = "UC-" + System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(30);

        Map<String, Object> userCoupon = new HashMap<>();
        userCoupon.put("id", userCouponId);
        userCoupon.put("userId", userId);
        userCoupon.put("couponId", couponId);
        userCoupon.put("status", "AVAILABLE");
        userCoupon.put("issuedAt", now.toString());
        userCoupon.put("expiresAt", expiresAt.toString());

        // 사용자 쿠폰 목록에 추가
        userCouponList.add(userCoupon);
        USER_COUPONS.put(userId, userCouponList);

        // 발급 수량 증가
        coupon.put("issuedQuantity", issuedQuantity + 1);

        return Map.of(
            "userCouponId", userCouponId,
            "couponId", couponId,
            "couponName", coupon.get("name"),
            "userId", userId,
            "status", "AVAILABLE",
            "remainingQuantity", totalQuantity - (issuedQuantity + 1),
            "success", true
        );
    }

    /**
     * GET /api/users/{userId}/coupons
     * 사용자 보유 쿠폰 조회 (만료 체크 포함)
     */
    @GetMapping("/users/{userId}/coupons")
    public Map<String, Object> getUserCoupons(@PathVariable String userId) {
        List<Map<String, Object>> userCouponList = USER_COUPONS.getOrDefault(userId, new ArrayList<>());

        // 쿠폰 만료 체크 및 상세 정보 추가
        List<Map<String, Object>> result = userCouponList.stream()
                .map(uc -> {
                    // 만료 체크
                    checkAndUpdateExpiration(uc);

                    Map<String, Object> coupon = COUPONS.get(uc.get("couponId"));
                    Map<String, Object> detail = new HashMap<>(uc);
                    detail.put("couponName", coupon.get("name"));
                    detail.put("discountRate", coupon.getOrDefault("discountRate", null));
                    detail.put("discountAmount", coupon.getOrDefault("discountAmount", null));
                    return detail;
                })
                .collect(Collectors.toList());

        return Map.of(
            "userId", userId,
            "coupons", result,
            "total", result.size()
        );
    }

    /**
     * POST /api/coupons/{userCouponId}/use
     * 쿠폰 사용 (주문 시 호출)
     */
    @Operation(summary = "쿠폰 사용", description = "주문 시 쿠폰을 사용 처리합니다 (AVAILABLE → USED)")
    @PostMapping("/coupons/{userCouponId}/use")
    public synchronized Map<String, Object> useCoupon(
            @Parameter(description = "사용자 쿠폰 ID") @PathVariable String userCouponId) {
        // 모든 사용자 쿠폰에서 해당 쿠폰 찾기
        Map<String, Object> targetCoupon = null;
        String userId = null;

        for (Map.Entry<String, List<Map<String, Object>>> entry : USER_COUPONS.entrySet()) {
            for (Map<String, Object> uc : entry.getValue()) {
                if (userCouponId.equals(uc.get("id"))) {
                    targetCoupon = uc;
                    userId = entry.getKey();
                    break;
                }
            }
            if (targetCoupon != null) break;
        }

        if (targetCoupon == null) {
            throw new CouponNotFoundException(userCouponId);
        }

        // 만료 체크
        checkAndUpdateExpiration(targetCoupon);

        String status = (String) targetCoupon.get("status");

        // 사용 가능 여부 체크
        if ("USED".equals(status)) {
            throw new CouponAlreadyUsedException();
        }
        if ("EXPIRED".equals(status)) {
            throw new CouponExpiredException();
        }

        // 쿠폰 사용 처리
        targetCoupon.put("status", "USED");
        targetCoupon.put("usedAt", LocalDateTime.now().toString());

        return Map.of(
            "userCouponId", userCouponId,
            "userId", userId,
            "couponId", targetCoupon.get("couponId"),
            "status", "USED",
            "success", true
        );
    }

    /**
     * POST /api/coupons/{userCouponId}/restore
     * 쿠폰 복원 (주문 취소 시 호출)
     */
    @Operation(summary = "쿠폰 복원", description = "주문 취소 시 쿠폰을 복원합니다 (USED → AVAILABLE)")
    @PostMapping("/coupons/{userCouponId}/restore")
    public synchronized Map<String, Object> restoreCoupon(
            @Parameter(description = "사용자 쿠폰 ID") @PathVariable String userCouponId) {
        // 모든 사용자 쿠폰에서 해당 쿠폰 찾기
        Map<String, Object> targetCoupon = null;
        String userId = null;

        for (Map.Entry<String, List<Map<String, Object>>> entry : USER_COUPONS.entrySet()) {
            for (Map<String, Object> uc : entry.getValue()) {
                if (userCouponId.equals(uc.get("id"))) {
                    targetCoupon = uc;
                    userId = entry.getKey();
                    break;
                }
            }
            if (targetCoupon != null) break;
        }

        if (targetCoupon == null) {
            throw new CouponNotFoundException(userCouponId);
        }

        String status = (String) targetCoupon.get("status");

        // 사용된 쿠폰만 복원 가능
        if (!"USED".equals(status)) {
            throw new CouponNotAvailableException(status);
        }

        // 만료 여부 체크 (만료된 쿠폰은 복원 불가)
        String expiresAtStr = (String) targetCoupon.get("expiresAt");
        if (expiresAtStr != null) {
            LocalDateTime expiresAt = LocalDateTime.parse(expiresAtStr);
            if (LocalDateTime.now().isAfter(expiresAt)) {
                throw new CouponExpiredException();
            }
        }

        // 쿠폰 복원 처리
        targetCoupon.put("status", "AVAILABLE");
        targetCoupon.put("usedAt", null);

        return Map.of(
            "userCouponId", userCouponId,
            "userId", userId,
            "couponId", targetCoupon.get("couponId"),
            "status", "AVAILABLE",
            "success", true,
            "message", "쿠폰이 복원되었습니다."
        );
    }

    /**
     * 쿠폰 만료 체크 및 상태 업데이트 (헬퍼 메서드)
     */
    private void checkAndUpdateExpiration(Map<String, Object> userCoupon) {
        String status = (String) userCoupon.get("status");

        // AVAILABLE 상태일 때만 만료 체크
        if ("AVAILABLE".equals(status)) {
            String expiresAtStr = (String) userCoupon.get("expiresAt");
            if (expiresAtStr != null) {
                LocalDateTime expiresAt = LocalDateTime.parse(expiresAtStr);
                if (LocalDateTime.now().isAfter(expiresAt)) {
                    userCoupon.put("status", "EXPIRED");
                }
            }
        }
    }

    /**
     * GET /api/coupons/{couponId}
     * 쿠폰 상세 조회 (발급 현황 포함)
     */
    @GetMapping("/coupons/{couponId}")
    public Map<String, Object> getCoupon(@PathVariable String couponId) {
        Map<String, Object> coupon = COUPONS.get(couponId);
        if (coupon == null) {
            throw new CouponNotFoundException(couponId);
        }

        return new HashMap<>(coupon);
    }
}