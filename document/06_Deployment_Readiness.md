# Docker 및 Vercel 배포 준비 점검서

작성일: 2026-10-27  
1차 구현 반영: 2026-10-28  
대상: `frontend`(React/CRA), `backend`(Spring Boot 4, Java 17)

## 0. 사용자가 먼저 준비할 것

코드 작업과 별개로 아래 항목은 서비스 소유자가 준비해야 한다.

1. Docker 백엔드를 실행할 호스팅 서비스와 계정을 선택한다.
2. 운영 PostgreSQL 인스턴스를 만들고 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 확보한다.
3. S3 호환 오브젝트 스토리지와 bucket을 준비한다.
4. 프론트·백엔드 도메인을 결정한다. 가능하면 `app.example.com`, `api.example.com`처럼 동일한 상위 도메인을 사용한다.
5. 결제·검색·블록체인 운영 키를 준비해 Vercel과 백엔드 호스팅 플랫폼의 Secret/Environment Variables에 직접 등록한다. 키를 Git이나 채팅에 붙여 넣지 않는다.
6. 기존 `application.properties`에 들어 있던 블록체인 private key와 RPC credential은 노출된 것으로 보고 폐기·재발급한다.
7. 로컬 Docker 검증을 위해 Docker Desktop을 설치한다.

## 0.1 1차 구현 완료 현황

- [x] Spring 공통/운영 설정을 환경변수 기반으로 분리
- [x] 실제 비밀값이 없는 backend/frontend `.env.example` 추가
- [x] 기존 코드의 블록체인 private key 및 RPC credential 제거
- [x] 프론트 API/WS 주소를 `frontend/src/config/api.js`로 중앙화
- [x] 로컬 전용 CORS annotation/filter를 제거하고 전역 환경변수 기반 CORS로 통합
- [x] WebSocket 허용 origin을 환경변수로 변경
- [x] backend multi-stage/non-root `Dockerfile` 및 `.dockerignore` 추가
- [x] 로컬 PostgreSQL 포함 `compose.yaml` 추가
- [x] Vercel SPA rewrite 및 루트 basename 설정
- [x] Actuator readiness/liveness 설정
- [x] H2 test profile로 백엔드 테스트의 외부 DB 의존 제거
- [x] GitHub Actions에서 frontend/backend/Docker 검증 추가
- [ ] 오브젝트 스토리지 이전(스토리지 서비스 선택 후 진행)
- [ ] Redis 세션 및 외부 message broker(운영 확장 방식 결정 후 진행)
- [ ] 스케줄러 분산 lock(운영 인스턴스 수 결정 후 진행)
- [ ] Flyway/Liquibase 초기 migration(현재 운영 DB schema 확인 후 진행)
- [x] 실제 Docker image/Compose 실행 및 readiness health check 검증

## 1. 권장 배포 구조

```text
사용자
  ├─ HTTPS/WSS → Vercel: React 정적 프론트엔드
  ├─ HTTPS     → Docker 호스트: Spring Boot REST API
  └─ WSS       → Docker 호스트: Spring STOMP/SockJS
                         ├─ 관리형 PostgreSQL
                         └─ 외부 오브젝트 스토리지(S3 호환 서비스 등)
```

- Vercel에는 `frontend`만 배포한다.
- `backend`는 WebSocket, `HttpSession`, 상시 실행 스케줄러가 있으므로 Vercel Functions로 옮기지 않고 장기 실행 가능한 Docker 호스트에 배포한다.
- 운영 DB는 컨테이너 내부 DB가 아니라 관리형 PostgreSQL을 우선 권장한다. `docker compose`의 DB는 로컬/통합 테스트 용도로만 사용한다.
- 가능하면 `app.example.com`과 `api.example.com`처럼 동일한 상위 도메인을 사용한다. 서로 완전히 다른 도메인을 쓰면 세션/CSRF 쿠키의 `SameSite=None; Secure` 설정과 브라우저 정책 대응이 더 까다롭다.

## 2. 현재 결론

1차 구현으로 배포 골격은 준비되었지만, 외부 인프라 연결과 운영 통합 검증 전에는 실제 서비스 배포를 승인하면 안 된다.

| 영역 | 상태 | 판단 근거 |
|---|---|---|
| 프론트 빌드 | 조건부 통과 | `npm.cmd run build` 성공, 미사용 import 경고 2건 |
| Vercel 설정 | 미준비 | `vercel.json`과 SPA rewrite가 없음 |
| 백엔드 컨테이너 | 미준비 | `Dockerfile`, `.dockerignore`, Compose 파일이 없음 |
| 운영 설정 | 차단 | `application.properties`가 Git에서 제외되고 기본 프로필이 `local`에 고정됨 |
| API 연결 | 차단 | 프론트 36개 소스 파일에 `localhost:8080` 또는 현재 호스트의 `:8080`이 하드코딩됨 |
| CORS/WebSocket | 차단 | 백엔드가 로컬 origin만 허용함 |
| 인증 세션 | 위험 | 메모리 기반 `HttpSession`; 다중 인스턴스와 교차 사이트 쿠키 설정 미비 |
| 파일 업로드 | 차단 | 컨테이너 로컬 `uploads/`에 저장하며 일부 URL도 `localhost`로 생성함 |
| DB/마이그레이션 | 차단 | 테스트가 실제 PostgreSQL 자격증명에 의존하고 Flyway/Liquibase가 없음 |
| 상태 확인/관측 | 미준비 | Actuator health check, 운영 로그/모니터링 설정이 없음 |
| CI | 미준비 | `.github/workflows`가 없으며 백엔드 전체 테스트가 현재 실패함 |

## 3. 우선순위별 작업

### P0 — 배포 전에 반드시 해결

#### 3.1 배포 설정을 Git에 포함하고 비밀값을 환경변수로 분리

현재 루트 `.gitignore`의 `application.properties` 규칙 때문에 `backend/src/main/resources/application.properties`도 추적되지 않는다. 따라서 새 서버나 Docker build context에는 이 파일이 들어가지 않는다. 또한 기본 활성 프로필이 `local`로 고정되어 있다.

해야 할 일:

1. 추적 가능한 공통 설정과 `application-prod.properties`를 만든다.
2. DB URL/계정, PKI 비밀값, Toss/PortOne 키, 검색 API 키, 블록체인 private key/RPC 주소를 `${ENV_NAME}` 형태로 바꾼다.
3. `spring.profiles.active=local`을 코드에서 제거하고 실행 시 `SPRING_PROFILES_ACTIVE=prod`로 주입한다.
4. `.env`는 계속 제외하되, 값이 없는 `.env.example`을 커밋한다.
5. 이미 저장소 이력이나 공유 채널에 실제 키가 노출되었다면 키를 폐기하고 재발급한다.

필수 운영 환경변수 후보:

```text
SPRING_PROFILES_ACTIVE
PORT
DB_URL
DB_USERNAME
DB_PASSWORD
FRONTEND_ORIGIN
PKI_SERVER_SECRET
TOSS_SECRET_KEY
PORTONE_PUBLIC_STORE_ID
PORTONE_PUBLIC_CHANNEL_KEY
PORTONE_API_SECRET
NAVER_CLIENT_ID
NAVER_CLIENT_SECRET
ALADIN_TTB_KEY
KAKAO_REST_API_KEY
GOOGLE_BOOKS_API_KEY
BLOCKCHAIN_URL
BLOCKCHAIN_PRIVATE_KEY
BLOCKCHAIN_CONTRACT_ADDRESS
```

#### 3.2 프론트 API/WS 주소를 단일 설정으로 통합

현재 최소 36개 프론트 소스 파일에서 로컬 주소를 직접 사용한다. Vercel에서는 사용자 브라우저의 `localhost:8080`에 요청하게 되므로 기능이 동작하지 않으며, HTTPS 페이지에서 HTTP API를 호출하면 mixed-content로 차단된다.

해야 할 일:

- `frontend/src/config/api.js` 같은 단일 모듈을 만들고 `REACT_APP_API_BASE_URL`, `REACT_APP_WS_URL`을 읽도록 한다.
- 모든 `fetch` 및 STOMP 연결이 이 모듈을 사용하게 한다.
- 운영값은 반드시 `https://` 및 `wss://`를 사용한다.
- Vercel 프로젝트의 Production/Preview 환경변수를 각각 등록한다.

우선 수정 대상은 `frontend/src/contexts/*`, `frontend/src/pages/**/*`, `frontend/src/components/layout/Header.jsx`이다. 현재 하드코딩이 이 영역 전반에 퍼져 있으므로 개별 치환보다 공통 API 클라이언트로 모으는 편이 재발을 막는다.

#### 3.3 운영 CORS, WebSocket origin, 쿠키 설정

수정 대상:

- `backend/src/main/java/com/Nbbang/backend/global/config/SecurityConfig.java`
- `backend/src/main/java/com/Nbbang/backend/domain/chat/config/WebSocketConfig.java`
- `backend/src/main/java/com/Nbbang/backend/domain/chat/config/ChatCorsConfig.java`
- `backend/src/main/java/com/Nbbang/backend/domain/payment/config/PaymentCorsConfig.java`
- 각 컨트롤러의 `@CrossOrigin(origins = "http://localhost:3000")`
- 신규 `backend/src/main/resources/application-prod.properties`

이유와 조치:

- 허용 origin을 `FRONTEND_ORIGIN`에서 읽도록 중앙화한다. 자격증명 요청에서는 `*`를 쓰면 안 된다.
- 컨트롤러별 `@CrossOrigin`을 제거하고 전역 정책 하나로 통일한다.
- WebSocket endpoint에도 운영 HTTPS origin을 명시한다.
- 프론트/백엔드가 교차 사이트라면 세션 쿠키에 `Secure`, `HttpOnly`, `SameSite=None`을 적용하고 CSRF 쿠키/헤더 흐름을 실제 브라우저에서 검증한다.
- 가능하면 동일 상위 도메인을 사용해 쿠키 복잡도를 줄인다.

#### 3.4 Docker 이미지 작성

추가 대상:

- `backend/Dockerfile`: Gradle build와 JRE 실행을 분리한 multi-stage image, Java 17, non-root 사용자, `PORT` 지원
- `backend/.dockerignore`: `.git`, `.gradle`, `build`, `.env`, IDE 파일, 로컬 `uploads` 제외
- 루트 `compose.yaml`: 로컬 검증용 backend + PostgreSQL, health check, named volume

컨테이너 명령은 `java -jar`로 실행하고 애플리케이션은 `server.port=${PORT:8080}`을 사용해야 한다. 이미지 안에 `.env`나 인증서를 COPY하지 않고 런타임 환경변수/secret으로 주입한다.

#### 3.5 업로드 저장소 외부화

수정 대상:

- `backend/src/main/java/com/Nbbang/backend/domain/product/service/ProductService.java`
- `backend/src/main/java/com/Nbbang/backend/domain/chat/controller/ChatRoomController.java`
- `backend/src/main/java/com/Nbbang/backend/global/config/WebMvcConfig.java`

현재 파일은 `${user.dir}/uploads`에 기록된다. 컨테이너 재배포 시 손실되고 여러 인스턴스에서 공유되지 않으며, 상품 이미지 URL은 `http://localhost:8080/uploads/...`로 저장된다.

운영에서는 S3 호환 오브젝트 스토리지에 업로드하고 공개 URL 또는 signed URL을 DB에 저장한다. 임시 조치로 볼륨을 마운트할 수 있지만 단일 호스트에 종속되므로 최종 구조로 보지 않는다.

#### 3.6 Vercel 설정 작성

추가 대상 `frontend/vercel.json` 예시:

```json
{
  "rewrites": [
    { "source": "/(.*)", "destination": "/index.html" }
  ]
}
```

Vercel 프로젝트 설정:

- Root Directory: `frontend`
- Install Command: `npm ci`
- Build Command: `npm run build`
- Output Directory: `build`
- Node.js 버전: 팀에서 고정한 LTS 버전
- 환경변수: `REACT_APP_API_BASE_URL`, `REACT_APP_WS_URL` 등

SPA rewrite가 없으면 `/login`, `/seller/dashboard` 같은 경로를 직접 열거나 새로고침할 때 404가 발생한다. `App.js`의 production `basename`은 기존 GitHub Pages 전제를 제거하고 Vercel 루트(`/`) 기준으로 검증해야 한다.

### P1 — 첫 운영 배포 전에 해결

#### 3.7 DB 스키마 마이그레이션과 테스트 격리

수정/추가 대상:

- `backend/build.gradle`: Flyway 또는 Liquibase, 테스트용 H2/Testcontainers 의존성
- `backend/src/main/resources/db/migration/*`: 버전 관리되는 스키마
- `backend/src/test/resources/application-test.properties`
- `backend/src/test/java/com/Nbbang/backend/BackendApplicationTests.java`

현재 `spring.jpa.hibernate.ddl-auto`에 의존하고, 전체 테스트가 로컬/외부 PostgreSQL로 접속한다. 실제 점검에서는 46개 중 1개가 `postgres` 사용자 인증 실패로 실패했다. 테스트는 운영 자격증명 없이 재현 가능해야 하며 배포 DB 변경은 마이그레이션으로 관리해야 한다. 운영 `ddl-auto`는 보통 `validate`를 사용한다.

#### 3.8 세션을 다중 인스턴스에 맞게 변경

현재 로그인과 WebSocket handshake가 서버 메모리의 `HttpSession`에 의존한다. 컨테이너를 둘 이상 실행하거나 재시작하면 로그인이 풀리거나 WebSocket 인증이 불일치할 수 있다.

선택지는 다음 중 하나다.

- 단기: 백엔드 1개 인스턴스만 운영하고 재배포 시 세션 종료를 허용한다.
- 권장: Spring Session + Redis로 세션을 공유한다.
- 장기 대안: 인증 구조를 검토해 짧은 수명의 토큰 방식으로 전환한다.

WebSocket의 `enableSimpleBroker`도 인스턴스 간 메시지를 공유하지 못하므로 수평 확장 시 외부 broker(RabbitMQ 등)를 검토한다.

#### 3.9 스케줄러의 중복 실행 방지

수정 대상:

- `backend/src/main/java/com/Nbbang/backend/domain/payment/scheduler/PaymentRefundScheduler.java`
- `backend/src/main/java/com/Nbbang/backend/domain/product/scheduler/SettlementScheduler.java`

두 작업은 매 인스턴스에서 60초마다 실행된다. 다중 인스턴스에서는 중복 환불/정산 위험이 있으므로 DB lock/ShedLock, 별도 worker, 또는 플랫폼 cron 중 하나로 단일 실행을 보장한다. 결제·정산 처리는 idempotency key와 DB 제약조건도 필요하다.

#### 3.10 health check와 운영 관측성

- `backend/build.gradle`에 Actuator를 추가한다.
- `/actuator/health/readiness`, `/actuator/health/liveness`만 외부 또는 플랫폼에 필요한 범위로 공개한다.
- Docker `HEALTHCHECK` 또는 호스팅 플랫폼 health endpoint를 연결한다.
- 비밀값/개인정보가 로그에 남지 않도록 운영 로그 레벨과 마스킹을 설정한다.
- 오류 추적, 구조화 로그, 알림 및 DB 백업/복구 절차를 정한다.

### P2 — 안정화 및 유지보수

#### 3.11 CI/CD 추가

추가 대상 `.github/workflows/ci.yml`:

1. `frontend`: `npm ci`, 테스트, production build
2. `backend`: Java 17 설정, `./gradlew test`, `./gradlew bootJar`
3. Docker image build 검사
4. main 배포 전 모든 단계 통과 강제

프론트 빌드 경고인 아래 미사용 import도 정리한다.

- `frontend/src/pages/seller/dashboard/SellerDashboardPage.jsx`
- `frontend/src/pages/seller/products/SellerProductsPage.jsx`

#### 3.12 의존성 및 이미지 운영 정책

- Gradle/Node 의존성 취약점 검사를 CI에 넣는다.
- Docker base image와 npm/Gradle 버전을 고정하고 정기적으로 갱신한다.
- production image에는 빌드 도구, 소스, `.env`, 테스트 결과물을 포함하지 않는다.
- 이미지 태그에 Git SHA를 사용하고 직전 버전 rollback 절차를 문서화한다.

## 4. 파일별 수정 계획 요약

| 파일/경로 | 작업 | 이유 |
|---|---|---|
| `.gitignore` | 공통/운영 Spring 설정 추적 정책 수정, 예제 env 허용 | 현재 핵심 설정이 배포 산출물에서 사라짐 |
| `backend/src/main/resources/application.properties` | 공통 비밀 없는 설정만 유지하고 Git 추적 | 깨끗한 checkout에서도 부팅 가능해야 함 |
| `backend/src/main/resources/application-prod.properties` | 운영 DB, port, cookie, CORS 관련 env placeholder 추가 | local/production 분리 |
| `backend/.env.example` | 필수 키 이름과 설명 추가 | 실제 비밀 없이 설정 계약 공유 |
| `backend/Dockerfile` | multi-stage/non-root 이미지 | 재현 가능한 백엔드 배포 |
| `backend/.dockerignore` | 비밀값·불필요 파일 제외 | 보안 및 이미지 크기 감소 |
| `compose.yaml` | 로컬 backend/DB/health/volume 구성 | Docker 환경 통합 검증 |
| `frontend/vercel.json` | SPA rewrite | 직접 접근/새로고침 404 방지 |
| `frontend/src/config/api.js` | API/WS URL 중앙화 | localhost 제거 및 환경별 배포 |
| `frontend/src/contexts/*`, `frontend/src/pages/**/*`, `Header.jsx` | 공통 API 설정 사용 | 하드코딩된 URL 제거 |
| `frontend/src/App.js` | Vercel 루트 기준 basename 검증 | GitHub Pages 전제 제거 |
| `SecurityConfig.java` 및 CORS 설정/어노테이션 | 환경변수 기반 단일 CORS 정책 | Vercel origin과 credential 허용 |
| `WebSocketConfig.java` | 운영 origin 및 WSS 검증 | 배포 후 실시간 채팅 연결 |
| `ProductService.java`, `ChatRoomController.java`, `WebMvcConfig.java` | 오브젝트 스토리지로 이전 | 컨테이너 파일 유실 방지 |
| `build.gradle` | Actuator, migration, 테스트 격리 의존성 정리 | health check 및 재현 가능한 배포 |
| `db/migration/*` | 초기 스키마부터 버전 관리 | 운영 DB 변경 안전성 |
| `application-test.properties` | 독립 테스트 DB 설정 | 실제 DB 자격증명 의존 제거 |
| 스케줄러 2개 | 단일 실행/멱등성 보장 | 중복 환불·정산 방지 |
| `.github/workflows/ci.yml` | 테스트/build/image 검증 | 실패한 버전의 배포 차단 |

## 5. 권장 실행 순서

1. 배포 도메인, Docker 호스팅, 관리형 PostgreSQL, 오브젝트 스토리지를 결정한다.
2. 환경변수 계약과 `application-prod.properties`를 만들고 Git ignore 문제를 해결한다.
3. 프론트의 모든 API/WS URL을 중앙화한다.
4. 운영 CORS, WebSocket origin, HTTPS 쿠키/CSRF를 설정한다.
5. 업로드를 오브젝트 스토리지로 이전한다.
6. Dockerfile과 Compose를 만들고 로컬에서 image build 및 health check를 검증한다.
7. DB migration과 독립 테스트 환경을 만들고 전체 테스트를 통과시킨다.
8. Vercel 설정과 환경변수를 등록해 Preview에서 로그인, 결제, 업로드, 채팅을 검증한다.
9. 세션/스케줄러의 단일 인스턴스 제한 또는 Redis/lock 전략을 확정한다.
10. CI를 통과한 버전만 production에 배포하고 rollback 및 백업 복구를 시험한다.

## 6. 배포 승인 체크리스트

- [ ] 저장소에 실제 비밀값이 없고 노출된 키는 회전했다.
- [ ] 깨끗한 checkout에서 `npm ci && npm run build`가 성공한다.
- [ ] 깨끗한 checkout에서 `./gradlew test && ./gradlew bootJar`가 성공한다.
- [ ] `docker build` 후 non-root 사용자로 정상 기동한다.
- [ ] health check가 DB 연결 포함 여부에 맞게 정상 응답한다.
- [ ] Vercel의 모든 주요 경로를 직접 열고 새로고침해도 404가 아니다.
- [ ] 브라우저에서 mixed-content/CORS/cookie/CSRF 오류가 없다.
- [ ] 로그인 유지, 로그아웃, 권한별 접근이 production domain에서 동작한다.
- [ ] 상품/채팅 이미지가 재배포 후에도 유지된다.
- [ ] `wss://` 채팅 연결, 재연결, 세션 인증이 동작한다.
- [ ] 결제 성공/실패 callback URL이 production URL이다.
- [ ] 스케줄러와 결제 요청의 중복 실행 방지가 검증되었다.
- [ ] DB migration, 백업, 복구, rollback 절차가 검증되었다.

## 7. 이번 점검에서 실행한 검증

- 프론트: production build 성공. `AlertTriangle` 미사용 import 경고 2건 존재.
- 백엔드: 총 46개 테스트 중 1개 실패. `BackendApplicationTests`가 외부 PostgreSQL의 `postgres` 사용자 인증 실패로 ApplicationContext를 올리지 못함.
- 배포 파일 검색: Dockerfile, Compose, `vercel.json`, GitHub Actions workflow 없음.
- Git 추적 확인: `backend/.env`, `application.properties`, `application-local.properties` 모두 제외됨.
- 2026-10-28 Docker 검증: backend image build 성공, PostgreSQL healthy, `/actuator/health/readiness`에서 `UP` 확인.

이 문서는 준비 상태 점검서이며 Docker/Vercel 설정이나 애플리케이션 코드는 아직 변경하지 않았다.
