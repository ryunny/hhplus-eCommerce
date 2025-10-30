flowchart TD
A["쿠폰 발급 시작"] --> B["쿠폰 선택"]
B --> C["쿠폰 수량 차감"]
C --> D{"차감 성공?"}
D -- No --> E["발급 마감"]
E --> Z["종료"]
D -- Yes --> F["사용자에게 쿠폰 발급"]
F --> G["발급 완료"]
G --> Z

    style A fill:#e1f5ff
    style D fill:#fff4e6
    style E fill:#ffe6e6
    style G fill:#d4edda