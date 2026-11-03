# 🛒 공동구매 예외처리

공동구매 시스템에서 마감 처리 시 발생하는 정상/예외 시나리오를 처리하는 Spring Boot 데모 프로젝트입니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| **Backend** | Spring Boot · Spring Data JPA |
| **DB** | MySQL 8.0 |
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

### 📌 관리자 액션 로그

관리자가 버튼을 누를 때마다 로그가 **두 곳에 동시 저장**됩니다.

| 저장 위치 | 경로 |
|-----------|------|
| 파일 | `logs/admin-actions.log` |
| DB | `ADMIN_LOG` 테이블 |

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
│   ├── AdminLog.java                  # 관리자 액션 로그 Entity
│   ├── ChatRoom.java                  # 채팅방 Entity (기능 구현 예정)
│   ├── ChatMessage.java               # 채팅 메시지 Entity (기능 구현 예정)
│   └── Scrap.java                     # 스크랩 Entity (기능 구현 예정)
└── dto/
    └── DeadlineResult.java            # 마감 처리 결과 DTO
```

---

## 시스템 요구사항

- Java 17 이상
- MySQL 8.0 이상

---

## 실행 방법

### Step 1. MySQL 설정

1. MySQL 서버를 실행합니다.
2. 아래 명령어로 데이터베이스를 생성합니다.
```sql
CREATE DATABASE gp_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
3. `src/main/resources/application.properties` 에서 비밀번호를 본인 MySQL 비밀번호로 변경합니다.
```properties
spring.datasource.password=your_password_here
```

### Step 2. 백엔드 실행 (Spring Boot)

1. IntelliJ IDEA에서 프로젝트 폴더를 엽니다.
2. `src/main/java/com/demo/gp/` 에서 `GroupPurchaseApplication.java`를 찾아 실행합니다.
3. 콘솔에 아래 메시지가 출력되면 성공입니다.
```
Started GroupPurchaseApplication in X.XXX seconds
```

백엔드 서버 : `http://localhost:8080`

### Step 3. 브라우저 접속

브라우저에서 `http://localhost:8080` 접속

---

## 주의사항

- `application.properties`의 `spring.datasource.password` 값을 본인 MySQL 비밀번호로 변경해야 합니다.
- `spring.jpa.hibernate.ddl-auto=create` 설정으로 서버 재시작 시 테이블이 초기화되고 `data.sql`이 다시 실행됩니다.
- 테스트용 로컬 환경에서만 동작하며, 실제 배포 시 HTTPS 및 보안 설정이 추가로 필요합니다.
