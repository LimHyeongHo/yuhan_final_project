# 🛒 공동구매 예외처리
 
공동구매 시스템에서 마감 처리 시 발생하는 정상/예외 시나리오를 처리하는 Spring Boot 데모 프로젝트입니다.
 
---
 
## 기술 스택
 
| 구분 | 기술 |
|------|------|
| **Backend** | Spring Boot · Spring Data JPA |
| **DB** | H2 인메모리 데이터베이스 |
| **View** | Thymeleaf |
 
---
 
## 주요 기능
 
### 📌 마감 처리 시나리오
 
**시나리오 A — 정상 처리**
- 마감 시 미입금자 자동 제외 및 경고 횟수 증가
- 결제 완료 인원 기준으로 1인당 금액 재계산 (N빵)
- 최소 인원 충족 시 → `모집중` → `발주대기` → `발주진행`
**시나리오 B — 예외 처리**
- 미입금자 제외 후 최소 인원 미달 시 → `모집 실패` 처리
- 결제 완료자 전원 자동 환불
### 📌 관리자 기능
 
| 액션 | 설명 |
|------|------|
| 발주 확정 | `발주대기` → `발주진행`, 구매 이력(History) 저장 |
| 마감 연장 | 미입금자 퇴출 + 단가 재계산 + 마감시간 1일 연장 |
| 실패 확정 | 공동구매 실패 처리, 결제자 전원 환불 |
| 데모 초기화 | 전체 데이터 삭제 후 초기 상태로 리셋 |
 
---
 
## 프로젝트 구조
 
```
src/main/java/com/demo/gp/
├── controller/
│   └── GroupPurchaseController.java   # 라우팅 및 요청 처리
├── service/
│   └── GroupPurchaseService.java      # 마감 처리 핵심 비즈니스 로직
├── entity/
│   ├── GroupPurchase.java             # 공동구매 Entity
│   ├── Participation.java             # 참여자 Entity
│   ├── Product.java                   # 상품 Entity
│   ├── User.java                      # 유저 Entity (ADMIN / SELLER / BUYER)
│   ├── History.java                   # 구매 이력 Entity
│   ├── ChatRoom.java                  # 채팅방 Entity (기능 구현 예정)
│   ├── ChatMessage.java               # 채팅 메시지 Entity (기능 구현 예정)
│   └── Scrap.java                     # 스크랩 Entity (기능 구현 예정)
└── dto/
    └── DeadlineResult.java            # 마감 처리 결과 DTO
```
 
---
 
## 시스템 요구사항
 
- Java 17 이상
---
 
## 실행 방법
 
### Step 1. 백엔드 실행 (Spring Boot)
 
1. IntelliJ IDEA에서 프로젝트 폴더를 엽니다.
2. `src/main/java/com/demo/gp/` 에서 `Application.java`를 찾아 실행합니다.
3. 콘솔에 아래 메시지가 출력되면 성공입니다.
```
Started Application in X.XXX seconds
```
 
백엔드 서버 : `http://localhost:8080`
 
### Step 2. 브라우저 접속
 
브라우저에서 `http://localhost:8080` 접속
 
> ⚠️ H2 인메모리 DB를 사용하므로 서버 재시작 시 데이터가 초기화됩니다.
 
---
 
## 주의사항
 
- 본 데모는 H2 인메모리 DB로 동작하며 서버 재시작 시 초기 데이터로 리셋됩니다.
- 테스트용 로컬 환경에서만 동작하며, 실제 배포 시 HTTPS 및 보안 설정이 추가로 필요합니다.
---
 
## 추가 구현 예정
 
- MySQL 연동으로 공동구매 상태, 참여자 정보 및 처리 이력 영구 저장
- 판매자/관리자 액션(발주 확정, 모집 실패 등) 감사 로그 자동 기록
- React 기반 프론트엔드로 전환
