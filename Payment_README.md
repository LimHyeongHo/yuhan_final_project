#  결제/정산 + CA 인증서 보안 파트



---

## 🛠 기술 스택
- Spring Boot
- Java 17
- Toss Payments API
- WebFlux (외부 API 호출)
- OpenSSL (모의 CA 및 인증서 생성)
- HTTPS / TLS (X.509 인증서 기반)

---

## ✅ 구현 기능

### 💳 결제/정산

#### 1. 1/N 자동 계산
- 총 금액과 참여 인원 입력 시 1인당 금액 자동 계산
- 1원 단위 나머지 처리 (대표자 부담)

#### 2. 토스페이먼츠 결제 연동
- 토스페이먼츠 샌드박스(테스트 환경) 연동
- 실제 돈 없이 결제 시뮬레이션 가능
- 결제 성공/실패 처리

#### 3. 중복 결제 방지
- 같은 주문번호로 중복 결제 시도 시 자동 차단
- "이미 처리된 주문입니다" 메시지 반환

#### 4. 취소 시 금액 자동 재계산
- 참여자 취소 시 남은 인원 기준으로 금액 자동 재계산
- 취소된 참여자 상태 표시

#### 5. 마감 시간 자동 제외
- 마감 시간 설정 가능 (분 단위)
- 마감 시간 초과 시 미입금자 자동 제외
- 1분마다 자동으로 마감 시간 체크

#### 6. 예외처리

**중복 결제 방지**
- 같은 주문번호로 결제 시도 시 자동 차단
- "이미 처리된 주문입니다" 메시지 반환

**취소 시 예외처리**
- 참여자 1명 이하일 때 취소 불가 처리
- "참여자가 1명 이하면 취소 불가합니다" 메시지 반환

**마감 시간 초과 예외처리**
- 마감 시간 지난 미입금자 자동 제외
- 자동 제외 시 남은 인원으로 금액 재계산
- 콘솔에 자동 제외 로그 기록

**결제 실패 예외처리**
- 네트워크 오류 발생 시 fail.html로 이동
- 실패 사유 화면에 표시

**입력값 검증**
- 참여 인원 2명 미만 입력 시 경고
- 금액 0원 이하 입력 시 경고
- 빈 항목 입력 시 경고

---

### 🔒 CA 인증서 및 HTTPS 보안

> 참고 문서: [Microsoft Learn - X.509 인증서](https://learn.microsoft.com/ko-kr/azure/iot-hub/reference-x509-certificates)

#### 7. 모의 CA 구현
- openssl을 사용하여 직접 CA 역할 수행
- CA 개인키 및 루트 인증서 생성
- CA 기반 서버 인증서 발급

#### 8. HTTPS 적용
- 발급받은 인증서를 Spring Boot에 적용
- http 접속 차단, https(8443포트)로만 접속 가능
- 결제 시 오가는 모든 데이터 암호화

#### 9. CA 인증서 대시보드
- 실제 인증서 파일에서 데이터를 읽어와 시각화
- 인증서 검증 과정 단계별 시뮬레이션
- 만료된 인증서 시뮬레이션 (실제 만료 여부 동적 처리)

---

## 📋 API 목록

### 결제/정산 API

| Method | URL | 설명 |
|--------|-----|------|
| POST | /api/payment/confirm | 결제 승인 |
| GET | /api/payment/calculate | 1/N 자동 계산 |
| GET | /api/payment/success | 결제 성공 처리 |
| GET | /api/payment/fail | 결제 실패 처리 |
| POST | /api/payment/cancel | 참여자 취소 + 재계산 |
| POST | /api/payment/save | 주문 저장 + 마감 시간 설정 |
| GET | /api/payment/order/{orderId} | 주문 상태 조회 |

### CA 인증서 API

| Method | URL | 설명 |
|--------|-----|------|
| GET | /api/payment/cert-info | 실제 서버 인증서 파일 읽어서 상세 정보 반환 |
| GET | /api/payment/cert-verify | 인증서 검증 과정 5단계 반환 |
| GET | /api/payment/cert-expired | 만료된 인증서 정보 반환 (실제 만료 여부 동적 처리) |

---

## ⚙️ 실행 방법

### 1. application.yml 생성

프로젝트 루트에 application.yml 파일 생성 후 아래 내용 붙여넣기

```yaml
spring:
  application:
    name: payment

server:
  port: 8443
  ssl:
    key-store: classpath:keystore.p12
    key-store-password: 123456
    key-store-type: PKCS12
    key-alias: payment

app:
  base-url: https://localhost:8443

toss:
  client-key: test_ck_6BYq7GWPVvvpYXJq0dbaVNE5vbo1
  secret-key: test_sk_Z61JOxRQVEbEQQ09q5MmrW0X9bAq
```

### 2. keystore.p12 생성

openssl이 설치되어 있어야 합니다. 터미널에서 순서대로 실행해주세요.

```bash
# CA 개인키 생성
openssl genrsa -out ca.key 2048

# CA 루트 인증서 생성
openssl req -new -x509 -days 365 -key ca.key -out ca.crt \
  -subj "/C=KR/ST=Seoul/O=YuhanCA/CN=Yuhan Root CA"

# 서버 개인키 생성
openssl genrsa -out server.key 2048

# 서버 인증서 발급 요청서 생성
openssl req -new -key server.key -out server.csr \
  -subj "/C=KR/ST=Seoul/O=YuhanPayment/CN=localhost"

# CA가 서버 인증서 발급
openssl x509 -req -days 365 -in server.csr \
  -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt

# Spring Boot용 PKCS12 변환
openssl pkcs12 -export -in server.crt -inkey server.key \
  -out keystore.p12 -name payment -CAfile ca.crt \
  -caname root -passout pass:123456

# keystore.p12를 프로젝트 resources 폴더에 복사
cp keystore.p12 src/main/resources/
```

### 3. 서버 실행
```
PaymentApplication.java 실행
```

### 4. 접속
```
https://localhost:8443/payment.html
```
⚠️ 브라우저 경고 뜨면 고급 → 안전하지 않음으로 이동 클릭

---

## 🧪 테스트 방법 (결제 없이)

### 결제 화면
```
https://localhost:8443/payment.html
```

### 결제 성공 화면 바로 확인
```
https://localhost:8443/success.html?amount=48000&orderName=치킨&participants=4
```

### 1/N 계산 테스트
```
https://localhost:8443/api/payment/calculate?totalAmount=48000&participants=4
https://localhost:8443/api/payment/calculate?totalAmount=50000&participants=3
```

### 마감 시간 자동 제외 테스트
```
Postman에서
POST https://localhost:8443/api/payment/save
?orderId=order_test
&totalAmount=48000
&participants=4
&deadlineMinutes=1

→ 1분 후 콘솔에서 자동 제외 확인
```

### CA 인증서 대시보드
```
https://localhost:8443/ca-dashboard.html

탭 1 → 인증서 정보 (실제 파일에서 읽어온 데이터)
탭 2 → 검증 과정 시뮬레이션
탭 3 → 만료 인증서 시뮬레이션
```

---

## ⚠️ 주의사항
- keystore.p12, *.key 파일은 개인키 포함으로 깃허브에 올리지 않음
- 토스페이먼츠 테스트 키로만 동작함 (실제 결제 없음)
- DB 연동은 하지 않았음
- 모의 CA라 브라우저 경고가 뜨지만 암호화 통신은 정상 작동

---

## 🔒 CA 기관이란?

CA(Certificate Authority)는 디지털 인증서를 발급하고 관리하는 신뢰할 수 있는 기관입니다.
Microsoft 공식 문서에 따르면, X.509 인증서는 사용자, 컴퓨터, 서비스 또는 디바이스를 나타내는 디지털 문서이며 CA에서 발급합니다.

### 인증서 구조 (X.509 v3 기준)

| 필드 | 설명 |
|------|------|
| 버전 (Version) | 인증서 버전 번호 (v1, v2, v3) |
| 일련번호 (Serial Number) | CA에서 발급한 인증서의 고유 번호 |
| 발급자 (Issuer) | 인증서를 발급한 CA의 고유 이름 |
| 유효기간 (Validity) | 인증서가 유효한 기간 (시작일 ~ 만료일) |
| 주체 (Subject) | 인증서 소유자의 고유 이름 |
| 공개키 (Public Key) | 인증서 소유자의 공개키 정보 |
| 서명 알고리즘 | CA가 서명에 사용한 암호화 알고리즘 |

### 장단점

**장점**
- 결제 시스템 보안 강화: 사용자 입력 데이터, API 키 등 모든 데이터 암호화 전송
- PKI 개념 직접 구현: CA 기관의 역할을 직접 구현하여 공개키 인프라 구조 이해
- 중간자 공격(MITM) 방지: 카페 와이파이 등 공공 네트워크에서 데이터 탈취 방지

**단점**
- 모의 CA 신뢰 문제: 브라우저가 신뢰하지 않아 경고 표시 (실제 서비스는 Let's Encrypt 사용 필요)
- 인증서 만료 관리: 유효기간 지나면 재발급 필요
- 로컬 환경 한계: keystore.p12 경로가 로컬에 고정되어 있어 환경마다 재생성 필요

---

## 📚 참고 자료
- [Microsoft Learn - X.509 인증서](https://learn.microsoft.com/ko-kr/azure/iot-hub/reference-x509-certificates)
- [RFC 5280 - X.509 인증서 국제 표준](https://tools.ietf.org/html/rfc5280)
- [토스페이먼츠 개발자 문서](https://developers.tosspayments.com)
- [OpenSSL 공식 문서](https://www.openssl.org/docs/)