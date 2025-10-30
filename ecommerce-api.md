# 상품 목록 조회
GET /api/products
Query:
  categoryId: string (optional)
  sort: "price" | "popularity" | "newest"
Response:
  products: [
    {
      productId: string,
      name: string,
      price: number,
      stock: number,
      categoryId: string,
      categoryName: string
    }
  ]

# 상품 카테고리 조회
GET /api/categories
Response:
  categories: [
    {
      categoryId: string,
      name: string
    }
  ]

# 상품 상세 조회
GET /api/products/{productId}
Response:
  product: {
    productId: string,
    name: string,
    description: string,
    price: number,
    stock: number,
    categoryId: string,
    categoryName: string
  }

# 인기 상품 조회
GET /api/products/top
Response:
  period: "3days",
  products: [
    {
      rank: number,
      productId: string,
      name: string,
      salesCount: number,
      revenue: number
    }
  ]

# 주문 생성
POST /api/orders
Request:
  userId: string
  items: [
    {
      productId: string,
      quantity: number
    }
  ]
  userCouponId: string (optional)
  recipientName: string
  shippingAddress: string
  shippingPhone: string
Response:
  orderId: string
  items: [
    {
      productId: string,
      name: string,
      quantity: number,
      unitPrice: number,
      subtotal: number
    }
  ]
  subtotalAmount: number
  discountAmount: number
  totalAmount: number
  status: "PENDING" | "COMPLETED"

# 결제 처리
POST /api/orders/{orderId}/payment
Request:
  userId: string
Response:
  orderId: string
  paidAmount: number
  remainingBalance: number
  status: "SUCCESS" | "FAILED"
  dataTransmission: "SUCCESS" | "FAILED"

# 쿠폰 발급 (선착순)
POST /api/coupons/{couponId}/issue
Request:
  userId: string
Response:
  userCouponId: string
  couponName: string
  couponType: string
  discountRate: number
  discountAmount: number
  expiresAt: string
  remainingQuantity: number

# 보유 쿠폰 조회
GET /api/users/{userId}/coupons
Response:
  coupons: [
    {
      userCouponId: string,
      couponName: string,
      couponType: string,
      discountRate: number,
      discountAmount: number,
      status: "AVAILABLE" | "USED" | "EXPIRED",
      expiresAt: string
    }
  ]

# 재고 조회
GET /api/products/{productId}/stock
Response:
  {
    productId: string
    stock: number
  }

# 주문 목록 조회
GET /api/users/{userId}/orders
Response:
  orders: [
    {
      id: string
      userId: string
      userCouponId: string
      recipientName: string
      shippingAddress: string
      shippingPhone: string
      totalAmount: number
      discountAmount: number
      finalAmount: number
      status: string
      createdAt: timestamp
    }
  ]

# 장바구니에 상품 추가
POST /api/cart/items
Request:
  userId: string
  productId: string
  quantity: number
Response:
  cartItemId: string
  productId: string
  productName: string
  price: number
  quantity: number
  subtotal: number

# 장바구니 조회
GET /api/users/{userId}/cart
Response:
  items: [
    {
      cartItemId: string
      productId: string
      productName: string
      price: number
      quantity: number
      stock: number
      subtotal: number
    }
  ]
  totalAmount: number

# 장바구니 상품 수량 변경
PATCH /api/cart/items/{cartItemId}
Request:
  quantity: number
Response:
  cartItemId: string
  productId: string
  quantity: number
  subtotal: number

# 장바구니 상품 삭제
DELETE /api/cart/items/{cartItemId}
Response:
  status: "SUCCESS"

# 주문 취소
POST /api/orders/{orderId}/cancel
Request:
  userId: string
  reason: string
Response:
  orderId: string
  status: "CANCELLED"
  refundAmount: number
  couponRestored: boolean
  stockRestored: boolean

# 주문 환불
POST /api/orders/{orderId}/refund
Request:
  userId: string
  reason: string
Response:
  orderId: string
  refundId: string
  refundAmount: number
  refundedAt: timestamp
  status: "REFUNDED"


