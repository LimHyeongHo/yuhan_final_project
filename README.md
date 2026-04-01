# 💰 결제/정산 파트


## 🛠 기술 스택
- Spring Boot
- Java 17
- Toss Payments API
- WebFlux (외부 API 호출)

---

## ✅ 구현 기능

### 1. 1/N 자동 계산
- 총 금액과 참여 인원 입력 시 1인당 금액 자동 계산
- 1원 단위 나머지 처리 (대표자 부담)

### 2. 토스페이먼츠 결제 연동
- 토스페이먼츠 샌드박스(테스트 환경) 연동
- 실제 돈 없이 결제 시뮬레이션 가능
- 결제 성공/실패 처리

### 3. 중복 결제 방지
- 같은 주문번호로 중복 결제 시도 시 자동 차단

### 4. 취소 시 금액 자동 재계산
- 참여자 취소 시 남은 인원 기준으로 금액 자동 재계산
- 취소된 참여자 상태 표시

### 5. 마감 시간 자동 제외
- 마감 시간 설정 가능 (분 단위)
- 마감 시간 초과 시 미입금자 자동 제외
- 1분마다 자동으로 마감 시간 체크

### 6. 예외처리

#### 중복 결제 방지
- 같은 주문번호로 결제 시도 시 자동 차단
- "이미 처리된 주문입니다" 메시지 반환

#### 취소 시 예외처리
- 참여자 1명 이하일 때 취소 불가 처리
- "참여자가 1명 이하면 취소 불가합니다" 메시지 반환

#### 마감 시간 초과 예외처리
- 마감 시간 지난 미입금자 자동 제외
- 자동 제외 시 남은 인원으로 금액 재계산
- 콘솔에 자동 제외 로그 기록

#### 결제 실패 예외처리
- 네트워크 오류 발생 시 fail.html로 이동
- 실패 사유 화면에 표시

#### 입력값 검증
- 참여 인원 2명 미만 입력 시 경고
- 금액 0원 이하 입력 시 경고
- 빈 항목 입력 시 경고
```

---


## 실행 방법

### 1. 토스페이먼츠 테스트 키 발급
```
https://developers.tosspayments.com 접속
→ 회원가입 (무료)
→ 테스트 키 발급
```

### 2. application.yml 설정
```
application-template.yml을 application.yml로 복사 후
본인 키 입력
```
```yaml
toss:
  client-key: 본인_클라이언트_키
  secret-key: 본인_시크릿_키
```

### 3. 서버 실행
```
PaymentApplication.java 실행
```

### 4. 접속
```
http://localhost:8080/payment.html
```

---

## 📋 API 목록

| Method | URL | 설명 |
|--------|-----|------|
| POST | /api/payment/confirm | 결제 승인 |
| GET | /api/payment/calculate | 1/N 자동 계산 |
| GET | /api/payment/success | 결제 성공 처리 |
| GET | /api/payment/fail | 결제 실패 처리 |
| POST | /api/payment/cancel | 참여자 취소 + 재계산 |
| POST | /api/payment/save | 주문 저장 + 마감 시간 설정 |
| GET | /api/payment/order/{orderId} | 주문 상태 조회 |

---

##  테스트 방법 (결제 없이)

### 결제 화면
```
http://localhost:8080/payment.html
```

### 결제 성공 화면 바로 확인
```
http://localhost:8080/success.html?amount=48000&orderName=치킨&participants=4
```

### 1/N 계산 테스트
```
http://localhost:8080/api/payment/calculate?totalAmount=48000&participants=4
http://localhost:8080/api/payment/calculate?totalAmount=50000&participants=3
```

### 마감 시간 자동 제외 테스트
```
Postman에서
POST http://localhost:8080/api/payment/save
?orderId=order_test
&totalAmount=48000
&participants=4
&deadlineMinutes=1

→ 1분 후 콘솔에서 자동 제외 확인
```

---

##  주의사항
- application.yml은 보안상 깃허브에 올리지 않습니다
- application-template.yml 참고해서 본인 키 넣어주세요
- 토스페이먼츠 테스트 키로만 동작합니다 (실제 결제 없음)
- DB연동은 하지 않았습니다
```

---
