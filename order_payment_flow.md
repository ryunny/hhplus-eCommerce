flowchart TD
A["상품선택"] --> B["장바구니"]
B --> C["재고확인"]
C --> D{"재고있음?"}
D -- No --> A
D -- Yes --> E["재고차감"]
E --> F["주문"]
F --> G{"쿠폰 사용?"}
G -- Yes --> H["쿠폰 검증"]
H --> I{"쿠폰 유효?"}
I -- No --> K["배송지 선택 및 입력"]
I -- Yes --> J["쿠폰 적용"]
J --> K
G -- No --> K
K --> L["결제 처리"]
L --> M{"결제 성공?"}
M -- No --> N{"재시도?"}
N -- Yes --> K
N -- No --> O["재고 복구"]
O --> P{"쿠폰 사용했나?"}
P -- Yes --> Q["쿠폰 복구"]
P -- No --> A
Q --> A
M -- Yes --> R["주문 완료"]
R --> S["데이터 플랫폼 전송"]

    style A fill:#e1f5ff
    style D fill:#fff4e6
    style I fill:#fff4e6
    style M fill:#fff4e6
    style P fill:#fff4e6
    style R fill:#d4edda
    style S fill:#cce5ff


