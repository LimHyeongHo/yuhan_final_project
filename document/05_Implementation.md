# 05. 구현 및 형상 관리 (Implementation & Version Control)

- 문서 기준일: 2026-10-21
- 기본 브랜치: `main`
- 통합 브랜치: `develop`
- 현재 자동 CI/CD: 미구성

## 1. 구현 기준

### 1.1 최소 변경 원칙

- 하나의 브랜치/작업은 하나의 명확한 문제를 해결한다.
- 요청받지 않은 인접 코드·포맷·이름을 함께 변경하지 않는다.
- 가능한 경우 버그를 재현하는 테스트를 먼저 추가하고, 수정 후 관련 테스트를 실행한다.
- 기존 변경 내용이 있는 작업트리에서는 본인 작업과 무관한 변경을 되돌리지 않는다.

### 1.2 프로젝트 구조

| 영역 | 위치 | 구현 스택 |
|---|---|---|
| 프런트엔드 | `frontend/` | React 19, React Router, STOMP/SockJS, Jest/Testing Library |
| 백엔드 | `backend/` | Java 17, Spring Boot 4, JPA, WebSocket, Web3j |
| 스마트 컨트랙트 | `backend/src/main/resources/contracts/` | Solidity `PriceVerification` |
| 요구사항 | `requirements/` | 보안·안정성·외부 API 요구사항 |
| 현행 프로젝트 문서 | `document/` | 문제, 요구사항, 설계, 아키텍처, 구현 규칙 |
| 참고 코드 | `legacy_code/` | 현행 제품 코드와 구분 |

## 2. 브랜치 전략

### 2.1 브랜치 역할

```text
main
 └─ develop
     ├─ feature/<kebab-case>
     └─ fix/<kebab-case>
```

| 브랜치 | 역할 | 직접 작업 |
|---|---|---|
| `main` | 배포 가능 스냅샷 | 금지, PR로만 반영 |
| `develop` | 팀 통합 기준 | 원칙적으로 금지, PR 또는 합의된 merge로 반영 |
| `feature/*` | 신규 기능·문서 | 허용 |
| `fix/*` | 버그·안정성 수정 | 허용 |

### 2.2 명명 규칙

- 형식: `feature/<kebab-case>` 또는 `fix/<kebab-case>`
- 허용: 영문 소문자, 숫자, 하이픈
- 금지: 대문자, 밑줄, 공백, `feature/DB/pki`처럼 추가 슬래시, camelCase
- 예시: `feature/payment-refund`, `feature/chat-report`, `fix/blockchain-stability`

### 2.3 현재 저장소와의 차이

새 규칙은 앞으로 생성할 브랜치에 적용한다. 현재 원격에는 다음과 같은 레거시 이름이 남아 있다.

- 대문자: `feature/BlockChain`, `feature/QnA`
- 추가 슬래시: `feature/DB/pki`
- camelCase: `feature/frontendEdit`, `feature/productBackend`
- 밑줄: `feature/auth_member`, `feature/ui_v2`, `fix/auth_2`

기존 브랜치를 이름만 맞추기 위해 일괄 수정하지 않는다. 병합/폐기 시점에 정리하고 새 작업부터 규칙을 적용한다.

## 3. 작업–PR–병합 흐름

### 3.1 작업 시작

```powershell
git fetch origin
git switch develop
git pull --ff-only origin develop
git switch -c feature/<kebab-case>
```

`develop`에 직접 커밋하지 않고 작업 브랜치를 만든다.

### 3.2 동기화·충돌 해결

```powershell
git fetch origin
git merge origin/develop
```

- 충돌은 자신의 작업 브랜치에서 해결한다.
- 충돌 해결 후 관련 테스트를 다시 실행한다.
- 기록 보존을 위해 통합 도중 히스토리를 rebase로 일괄 재작성하지 않는다.

### 3.3 Pull Request

PR에는 다음을 포함한다.

1. 문제와 해결 방식
2. 변경한 파일과 영향 범위
3. 수행한 테스트·수동 검증과 결과
4. 수행하지 못한 검증과 사유
5. DB·외부 API·환경 변수·배포에 필요한 후속 조치
6. UI 변경이 있으면 전/후 캡처

최소 1명의 동료 리뷰를 권장한다. 다만 현재 저장소에서 GitHub branch protection으로 강제됨을 확인한 것은 아니므로, “필수”로 운영하려면 GitHub 설정을 추가해야 한다.

### 3.4 병합

- 기본 옵션: GitHub **Create a merge commit**
- 방향: 작업 브랜치 → `develop`
- 현 전략에서는 `Squash and merge`, `Rebase and merge`를 사용하지 않는다.
- 병합 전 최종 diff와 테스트 결과를 확인한다.

최근 `develop` 히스토리에는 PR Merge commit이 남아 있어 이 병합 방식을 대체로 사용하고 있음을 확인했다. GitHub에서 다른 병합 방식을 기술적으로 차단했는지는 별도 확인이 필요하다.

## 4. 커밋 규칙

### 4.1 커밋 메시지

형식:

```text
<type>: <한국어 또는 영문 요약>
```

| Type | 용도 | 예시 |
|---|---|---|
| `feat` | 기능 추가 | `feat: 채팅 신고 기능 추가` |
| `fix` | 버그 수정 | `fix: 환불 중복 호출 방지` |
| `test` | 테스트 | `test: 결제 멱등성 케이스 추가` |
| `docs` | 문서 | `docs: 현행 ERD 갱신` |
| `refactor` | 동작 변경 없는 구조 개선 | `refactor: 결제 상태 전환 분리` |
| `chore` | 빌드·설정·기타 | `chore: 테스트 설정 갱신` |

- 제목은 한 가지 목적을 명확히 적는다.
- 서로 되돌릴 이유가 다른 변경은 별도 커밋으로 나눈다.
- 비밀값, 개인정보, 실제 결제/계좌 정보를 커밋 메시지나 테스트 로그에 넣지 않는다.

### 4.2 커밋 단위

- 빌드되지 않는 중간 상태를 공유 브랜치에 올리지 않는다.
- 기능 코드와 해당 테스트/문서 변경은 같은 PR에 포함할 수 있다.
- 무관한 포맷 변경으로 실제 diff를 가리지 않는다.

## 5. 테스트·검증

### 5.1 프런트엔드

```powershell
Set-Location frontend
npm test -- --watchAll=false
npm run build
```

### 5.2 백엔드

```powershell
Set-Location backend
./gradlew.bat test
```

### 5.3 변경 유형별 추가 검증

| 변경 | 필수 검증 |
|---|---|
| DB 엔티티 | 마이그레이션 영향, nullable/기본값, 레거시 행 호환성 |
| 결제/환불 | 중복 콜백, 응답 유실, 승인 후 참여 실패, 취소 재시도 |
| 참여 인원 | 동시 요청, 마지막 1석, 중복 참여, 취소 |
| 인증/권한 | 비로그인, 역할 불일치, CSRF, 탈퇴, 만료/폐기 인증서 |
| WebSocket | 비인증 연결, 다른 채팅방 접근, 재연결, 전송 실패 |
| 블록체인 | RPC 장애, nonce 충돌, 영수증 대기, hash mismatch, 재시도 한도 |
| UI | 로딩, 빈 상태, 오류, 권한별 라우트, 반응형 화면 |

### 5.4 2026-10-21 기준 검증 상태

- 프런트: 12개 suite 중 11개 통과, 33개 test 중 32개 통과
- 프런트 실패: `App.test.js`가 이전 브랜드명 `YU-BOOK`을 기대하지만 현재 UI는 `N-BBANG`을 사용
- 백엔드: Gradle 9.4.1 배포본 다운로드 연결 재설정으로 테스트 실행 미완료
- CI: `.github/workflows` 없음

테스트 미실행은 통과로 간주하지 않는다. 도구/네트워크 문제로 못 돌렸다면 PR에 사유와 미검증 범위를 적는다.

## 6. 비밀정보·설정 관리

- `application.properties`, `application-local.properties`, `.env` 등의 실제 비밀값은 Git에 커밋하지 않는다.
- DB 인증정보, PortOne/Toss 키, RPC 키, 블록체인 개인키, PKI 서버 비밀값은 환경 변수 또는 Secret Manager로 주입한다.
- 평문 시크릿을 발견하면 파일 삭제만으로 끝내지 않고 해당 키를 폐기/재발급한다.
- 문서/예제에는 빈 플레이스홀더만 있는 `application-example.properties` 형식을 사용한다.
- 로그에 비밀값, 전체 결제 키, 계좌번호, CI 원문을 출력하지 않는다.

## 7. 브랜치 수명주기·정리

### 7.1 병합 후

```powershell
git fetch --prune origin
git switch develop
git pull --ff-only origin develop
git branch -d feature/<kebab-case>
```

- GitHub PR에서 병합 후 원격 브랜치를 삭제한다.
- 로컬 브랜치는 병합 확인 후 `-d`로 삭제한다. 안전 검사를 건너뛰는 `-D`는 일반 정리에 사용하지 않는다.

### 7.2 현재 청소 필요 상태

`origin/develop`에 이미 병합된 수십 개의 원격 `feature/*`, `fix/*` 브랜치가 남아 있다. 보호 대상·미병합 커밋·외부 참조 여부를 확인한 뒤 GitHub에서 단계적으로 정리한다. 이 문서는 브랜치 삭제 승인을 대신하지 않는다.

## 8. 장애 대응

| 문제 | 처리 원칙 |
|---|---|
| Merge conflict | 작업 브랜치에서 `origin/develop`을 merge하고 관련 테스트 재실행 |
| Broken build | 해당 PR을 병합하지 않고 재현 로그·환경·실패 테스트를 기록 |
| 외부 API 장애 | 타임아웃·재시도·상태 대조·사용자 안내를 구분 |
| DB 마이그레이션 실패 | nullable/기본값/기존 행 호환성을 확인하고 복구 절차를 마련 |
| 비밀정보 노출 | 즉시 키 폐기/재발급, 유출 범위 확인, Git 히스토리·로그 점검 |
| 블록체인 지연 | DB 작업과 분리한 비동기 상태로 관리하고 `PENDING/UNAVAILABLE/FAILED` 상태를 사용자에게 구분해 표시 |

## 9. Definition of Done

다음을 모두 충족해야 작업 완료로 본다.

- [ ] 요구사항과 인수 기준을 충족했다.
- [ ] 관련 코드·테스트·문서만 변경했다.
- [ ] 자동 테스트와 필요한 수동 검증을 수행했다.
- [ ] 미실행 검증과 남은 위험을 PR에 기록했다.
- [ ] 비밀정보·개인정보·불필요한 로그가 diff에 없다.
- [ ] DB/API/환경 변수 호환성과 되돌리기 방법을 확인했다.
- [ ] 동료 리뷰 후 Create a merge commit으로 `develop`에 병합했다.

## 10. 후속 개선 항목

1. GitHub Actions에 프런트/백엔드 테스트·빌드·시크릿 스캔을 구성한다.
2. `main`, `develop`에 PR/리뷰/테스트 통과를 강제하는 branch protection을 적용한다.
3. 병합 후 원격 브랜치 자동 삭제를 활성화한다.
4. 남아 있는 레거시 브랜치를 검토한 뒤 정리한다.
5. 프런트 `App.test.js`의 브랜드명 기대값을 현재 UI와 맞춘다.

## 11. 관련 문서

- [`01_Problem_Definition.md`](./01_Problem_Definition.md): 문제·범위·KPI
- [`02_Requirements.md`](./02_Requirements.md): 요구사항·인수 기준
- [`03_Design.md`](./03_Design.md): 역할·데이터·상태 설계
- [`04_Architecture.md`](./04_Architecture.md): 논리·배포 아키텍처
