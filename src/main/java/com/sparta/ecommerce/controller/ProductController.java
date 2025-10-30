package com.sparta.ecommerce.controller;

import com.sparta.ecommerce.exception.InsufficientStockException;
import com.sparta.ecommerce.exception.ProductNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/products")
@Tag(name = "상품 관리", description = "상품 조회 및 재고 관리 API")
public class ProductController {

    // 메모리 상품 데이터 (재고 관리)
    private static final Map<String, Map<String, Object>> PRODUCTS = new ConcurrentHashMap<>();

    // 판매 통계 데이터 (productId -> salesCount)
    private static final Map<String, Integer> SALES_COUNT = new ConcurrentHashMap<>();

    static {
        // 초기 상품 데이터
        Map<String, Object> p1 = new HashMap<>();
        p1.put("id", "P001");
        p1.put("name", "노트북");
        p1.put("price", 1500000);
        p1.put("stock", 10);
        p1.put("category", "전자제품");
        p1.put("description", "고성능 게이밍 노트북");
        PRODUCTS.put("P001", p1);
        SALES_COUNT.put("P001", 150);

        Map<String, Object> p2 = new HashMap<>();
        p2.put("id", "P002");
        p2.put("name", "키보드");
        p2.put("price", 120000);
        p2.put("stock", 50);
        p2.put("category", "주변기기");
        p2.put("description", "기계식 무선 키보드");
        PRODUCTS.put("P002", p2);
        SALES_COUNT.put("P002", 320);

        Map<String, Object> p3 = new HashMap<>();
        p3.put("id", "P003");
        p3.put("name", "마우스");
        p3.put("price", 80000);
        p3.put("stock", 30);
        p3.put("category", "주변기기");
        p3.put("description", "게이밍 무선 마우스");
        PRODUCTS.put("P003", p3);
        SALES_COUNT.put("P003", 280);

        Map<String, Object> p4 = new HashMap<>();
        p4.put("id", "P004");
        p4.put("name", "모니터");
        p4.put("price", 450000);
        p4.put("stock", 15);
        p4.put("category", "전자제품");
        p4.put("description", "27인치 4K 모니터");
        PRODUCTS.put("P004", p4);
        SALES_COUNT.put("P004", 95);

        Map<String, Object> p5 = new HashMap<>();
        p5.put("id", "P005");
        p5.put("name", "헤드셋");
        p5.put("price", 200000);
        p5.put("stock", 25);
        p5.put("category", "주변기기");
        p5.put("description", "노이즈 캔슬링 헤드셋");
        PRODUCTS.put("P005", p5);
        SALES_COUNT.put("P005", 180);

        Map<String, Object> p6 = new HashMap<>();
        p6.put("id", "P006");
        p6.put("name", "웹캠");
        p6.put("price", 150000);
        p6.put("stock", 20);
        p6.put("category", "주변기기");
        p6.put("description", "1080p 웹캠");
        PRODUCTS.put("P006", p6);
        SALES_COUNT.put("P006", 120);
    }

    /**
     * GET /api/products
     * 상품 목록 조회 (카테고리 필터링, 정렬 지원)
     */
    @Operation(summary = "상품 목록 조회", description = "카테고리 필터링 및 정렬 기능을 제공합니다")
    @GetMapping
    public Map<String, Object> getProducts(
            @Parameter(description = "카테고리 필터 (예: 전자제품, 주변기기)")
            @RequestParam(required = false) String category,
            @Parameter(description = "정렬 방식 (price: 가격순, popularity: 인기순, newest: 최신순)")
            @RequestParam(required = false) String sort) {

        List<Map<String, Object>> productList = new ArrayList<>(PRODUCTS.values());

        // 카테고리 필터링
        if (category != null && !category.isEmpty()) {
            productList = productList.stream()
                    .filter(p -> category.equals(p.get("category")))
                    .collect(java.util.stream.Collectors.toList());
        }

        // 정렬
        if (sort != null) {
            switch (sort) {
                case "price":
                    // 가격 오름차순
                    productList.sort((p1, p2) -> {
                        int price1 = (int) p1.get("price");
                        int price2 = (int) p2.get("price");
                        return Integer.compare(price1, price2);
                    });
                    break;
                case "popularity":
                    // 인기순 (판매량 내림차순)
                    productList.sort((p1, p2) -> {
                        int sales1 = SALES_COUNT.getOrDefault((String) p1.get("id"), 0);
                        int sales2 = SALES_COUNT.getOrDefault((String) p2.get("id"), 0);
                        return Integer.compare(sales2, sales1); // 내림차순
                    });
                    break;
                case "newest":
                    // 최신순 (ID 역순으로 간단히 구현)
                    productList.sort((p1, p2) -> {
                        String id1 = (String) p1.get("id");
                        String id2 = (String) p2.get("id");
                        return id2.compareTo(id1); // 역순
                    });
                    break;
            }
        }

        return Map.of("products", productList);
    }

    /**
     * GET /api/products/top
     * 인기 상품 Top 5 (최근 3일 판매량 기준)
     */
    @Operation(summary = "인기 상품 Top 5", description = "최근 3일간 판매량 기준 인기 상품 Top 5를 조회합니다")
    @GetMapping("/top")
    public Map<String, Object> getTopProducts() {
        List<Map<String, Object>> topProducts = new ArrayList<>();

        // 판매량 기준으로 정렬
        List<Map.Entry<String, Integer>> sortedSales = new ArrayList<>(SALES_COUNT.entrySet());
        sortedSales.sort((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()));

        // Top 5 추출
        int rank = 1;
        for (int i = 0; i < Math.min(5, sortedSales.size()); i++) {
            Map.Entry<String, Integer> entry = sortedSales.get(i);
            String productId = entry.getKey();
            int salesCount = entry.getValue();

            Map<String, Object> product = PRODUCTS.get(productId);
            if (product != null) {
                int price = (int) product.get("price");
                Map<String, Object> topProduct = new HashMap<>();
                topProduct.put("rank", rank++);
                topProduct.put("productId", productId);
                topProduct.put("name", product.get("name"));
                topProduct.put("salesCount", salesCount);
                topProduct.put("revenue", price * salesCount);
                topProducts.add(topProduct);
            }
        }

        return Map.of(
            "period", "3days",
            "products", topProducts
        );
    }

    /**
     * GET /api/products/{productId}
     * 상품 상세 조회
     */
    @GetMapping("/{productId}")
    public Map<String, Object> getProduct(@PathVariable String productId) {
        Map<String, Object> product = PRODUCTS.get(productId);
        if (product == null) {
            throw new ProductNotFoundException(productId);
        }
        return Map.of("product", product);
    }

    /**
     * POST /api/products/{productId}/stock/decrease
     * 재고 차감
     */
    @Operation(
        summary = "재고 차감",
        description = "주문 시 상품 재고를 차감합니다 (동시성 제어)",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "재고 차감 요청",
            content = @io.swagger.v3.oas.annotations.media.Content(
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\"quantity\": 3}"
                )
            )
        )
    )
    @PostMapping("/{productId}/stock/decrease")
    public synchronized Map<String, Object> decreaseStock(
            @Parameter(description = "상품 ID") @PathVariable String productId,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> product = PRODUCTS.get(productId);
        if (product == null) {
            throw new ProductNotFoundException(productId);
        }

        int quantity = (int) request.get("quantity");
        int currentStock = (int) product.get("stock");

        // 재고 부족 체크
        if (currentStock < quantity) {
            throw new InsufficientStockException(currentStock, quantity);
        }

        // 재고 차감
        product.put("stock", currentStock - quantity);

        return Map.of(
            "productId", productId,
            "productName", product.get("name"),
            "decreasedQuantity", quantity,
            "remainingStock", product.get("stock"),
            "success", true
        );
    }

    /**
     * GET /api/products/{productId}/stock
     * 재고 조회
     */
    @GetMapping("/{productId}/stock")
    public Map<String, Object> getStock(@PathVariable String productId) {
        Map<String, Object> product = PRODUCTS.get(productId);
        if (product == null) {
            throw new ProductNotFoundException(productId);
        }

        return Map.of(
            "productId", productId,
            "productName", product.get("name"),
            "stock", product.get("stock")
        );
    }

    /**
     * POST /api/products/{productId}/stock/increase
     * 재고 복원 (결제 실패/주문 취소 시)
     */
    @Operation(
        summary = "재고 복원",
        description = "결제 실패 또는 주문 취소 시 재고를 복원합니다",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "재고 복원 요청",
            content = @io.swagger.v3.oas.annotations.media.Content(
                examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                    value = "{\"quantity\": 3}"
                )
            )
        )
    )
    @PostMapping("/{productId}/stock/increase")
    public synchronized Map<String, Object> increaseStock(
            @Parameter(description = "상품 ID") @PathVariable String productId,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> product = PRODUCTS.get(productId);
        if (product == null) {
            throw new ProductNotFoundException(productId);
        }

        int quantity = (int) request.get("quantity");
        int currentStock = (int) product.get("stock");

        // 재고 증가
        product.put("stock", currentStock + quantity);

        return Map.of(
            "productId", productId,
            "productName", product.get("name"),
            "increasedQuantity", quantity,
            "currentStock", product.get("stock"),
            "success", true
        );
    }
}
