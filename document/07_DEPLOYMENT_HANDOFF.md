# Docker + Vercel 실제 공개 배포 인수인계서

작성일: 2026-10-28  
프로젝트: N-BBANG (`yuhan_final_project`)  
GitHub: `https://github.com/LimHyeongHo/yuhan_final_project`  
현재 배포 브랜치: `depoly/test`

> 중요: 이 저장소에서 커밋, push, merge, rebase는 반드시 사용자에게 먼저 확인받고 진행한다. 파일 수정과 로컬 테스트가 끝나도 자동으로 커밋하지 않는다.

## 1. 문서 목적

이 문서는 새 노트북이나 새로운 Codex 세션에서 현재 배포 작업을 그대로 이어가기 위한 인수인계 문서다. 다음 내용을 순서대로 설명한다.

1. 개발 환경 설치
2. GitHub 저장소와 배포 브랜치 받기
3. 환경변수 준비
4. 로컬 Docker 검증
5. Vercel 프론트엔드 공개 배포
6. Render Docker 백엔드 및 PostgreSQL 공개 배포
7. Vercel과 Render 연결
8. 배포 후 검증
9. 단계별 오류 해결

## 2. 현재까지 완료된 내용

### 2.1 Git 상태

현재 배포 변경은 GitHub의 `depoly/test` 브랜치에 올라가 있다.

```text
5bcc451 fix: configure Vercel deployment from repository root
9dac0e0 chore: prepare Docker and Vercel deployment
```

`main` 브랜치는 현재 배포 변경을 포함하지 않는다. Vercel과 Render는 반드시 `depoly/test`를 사용하거나, 사용자가 직접 검토 후 해당 브랜치를 `main`에 merge해야 한다.

### 2.2 구현된 배포 파일

| 파일 | 역할 |
|---|---|
| `backend/Dockerfile` | Spring Boot multi-stage/non-root Docker image |
| `backend/.dockerignore` | Docker image에서 비밀값과 불필요한 파일 제외 |
| `compose.yaml` | 로컬 PostgreSQL + backend 통합 실행 |
| `vercel.json` | 저장소 루트 기준 frontend 설치/build/output/SPA rewrite |
| `render.yaml` | Render Docker Web Service + PostgreSQL Blueprint |
| `backend/.env.example` | 백엔드 환경변수 이름 예시 |
| `frontend/.env.example` | 프론트 환경변수 이름 예시 |
| `backend/src/main/resources/application-prod.properties` | Spring 운영 profile |
| `frontend/src/config/api.js` | 프론트 API/WebSocket 주소 중앙화 |
| `.github/workflows/ci.yml` | frontend/backend/Docker CI |

### 2.3 완료된 검증

- 프론트 테스트: 12 suites, 33 tests 통과
- 프론트 production build 성공
- 백엔드 테스트 및 `bootJar` 성공
- Docker backend image build 성공
- 로컬 PostgreSQL 컨테이너 `healthy`
- `http://localhost:8080/actuator/health/readiness`에서 `{"status":"UP"}` 확인

### 2.4 현재 외부 배포 상태

- Vercel 프로젝트 도메인 후보: `https://yuhan-final-project-fc2q.vercel.app`
- Vercel Production Branch에 `depoly/test`를 입력하는 단계까지 진행
- Vercel에 아직 `depoly/test` 브랜치 배포 기록이 없어 `No deployments found for "depoly/test"` 메시지가 발생
- Render backend/PostgreSQL은 아직 생성하지 않음
- Vercel과 Render 환경변수 연결은 아직 완료하지 않음

## 3. 새 노트북 기본 환경 설정

### 3.1 필요한 프로그램

- Git
- Node.js LTS와 npm
- Java 17
- Docker Desktop
- VS Code 또는 사용하는 IDE

설치 확인:

```powershell
git --version
node --version
npm --version
java -version
docker --version
docker compose version
```

### 3.2 Docker Desktop 설치

Windows 설치 화면에서는 다음을 선택한다.

```text
Per-user installation
WSL 2
Add shortcut to desktop (선택 가능)
Allow Windows containers (선택하지 않음)
```

Docker Desktop 하단에 `Engine running`이 표시되어야 한다.

#### 설치 중 Virtual Machine Platform 오류가 날 때

관리자 권한 PowerShell에서 실행한다.

```powershell
wsl --install
```

실행 후 Windows를 재시작하고 Docker Desktop 설치를 다시 진행한다.

그래도 실패하면 관리자 PowerShell에서 실행한다.

```powershell
dism.exe /online /enable-feature /featurename:VirtualMachinePlatform /all /norestart
dism.exe /online /enable-feature /featurename:Microsoft-Windows-Subsystem-Linux /all /norestart
```

재시작 후 작업 관리자 → 성능 → CPU에서 `가상화: 사용`인지 확인한다. 사용 안 함이면 BIOS/UEFI에서 Intel VT-x 또는 AMD-V/SVM을 활성화해야 한다.

#### Docker 설치 후 명령어를 찾지 못할 때

오류 예시:

```text
docker: The term 'docker' is not recognized
```

VS Code와 모든 PowerShell 창을 완전히 종료한 후 다시 실행한다. 설치 전 터미널이 이전 PATH를 유지해서 발생하는 문제다.

사용자별 설치의 일반적인 실행 경로:

```text
%LOCALAPPDATA%\Programs\DockerDesktop\resources\bin
```

## 4. 저장소 받기

노트북 PowerShell에서 실행한다.

```powershell
git clone https://github.com/LimHyeongHo/yuhan_final_project.git
cd yuhan_final_project
git switch depoly/test
git pull origin depoly/test
git status
```

다음 명령으로 현재 배포 설정 커밋을 확인한다.

```powershell
git log -3 --oneline
```

최소한 `5bcc451`, `9dac0e0`이 보여야 한다.

### 브랜치를 찾지 못할 때

```powershell
git fetch origin --prune
git switch --track origin/depoly/test
```

## 5. 환경변수 준비

### 5.1 보안 원칙

- `.env`를 Git에 추가하지 않는다.
- API secret, DB password, blockchain private key를 채팅이나 문서에 작성하지 않는다.
- `REACT_APP_*` 값은 최종 JavaScript에 포함되어 브라우저에 공개된다.
- `TOSS_SECRET_KEY`, `PORTONE_API_SECRET`, `DB_PASSWORD`, `PKI_SERVER_SECRET`, `BLOCKCHAIN_PRIVATE_KEY`는 Render에만 등록한다.
- 과거 코드에 들어 있던 blockchain private key와 RPC credential은 폐기하고 새로 발급한다.

### 5.2 로컬 backend 환경변수

예제 파일을 복사한다.

```powershell
Copy-Item backend\.env.example backend\.env
```

`backend/.env`의 `change-me` 항목을 로컬 개발값으로 바꾼다. 이 파일은 Git에 commit하지 않는다.

### 5.3 로컬 frontend 환경변수

로컬에서는 기본값이 `http://localhost:8080`이므로 반드시 파일을 만들 필요는 없다. 명시적으로 관리하려면 다음 파일을 만든다.

```powershell
Copy-Item frontend\.env.example frontend\.env.local
```

기본값:

```env
REACT_APP_API_BASE_URL=http://localhost:8080
REACT_APP_WS_URL=http://localhost:8080/ws
REACT_APP_TOSS_CLIENT_KEY=실제_클라이언트키
```

## 6. 로컬 Docker 실행 및 검증

프로젝트 루트에서 실행한다.

```powershell
docker compose up --build -d
docker compose ps
```

정상 상태:

```text
db       healthy
backend  healthy 또는 starting 후 healthy
```

Health check:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health/readiness
```

예상 응답:

```json
{"status":"UP"}
```

로그 확인:

```powershell
docker compose logs -f backend
docker compose logs -f db
```

종료:

```powershell
docker compose down
```

DB volume까지 삭제하는 명령은 데이터가 모두 지워지므로 신중히 사용한다.

```powershell
docker compose down -v
```

### Docker credential helper 오류

오류 예시:

```text
docker-credential-desktop: executable file not found in %PATH%
```

VS Code를 완전히 재시작한다. 즉시 해결해야 한다면 현재 PowerShell에 경로를 임시 추가한다.

```powershell
$dockerBin = "$env:LOCALAPPDATA\Programs\DockerDesktop\resources\bin"
$env:Path = "$dockerBin;$env:Path"
docker compose up --build -d
```

## 7. 프론트 로컬 실행

```powershell
cd frontend
npm ci
npm test -- --watchAll=false
npm run build
npm start
```

접속 주소:

```text
http://localhost:3000
```

`react-router-dom`을 찾지 못하는 테스트 오류가 발생하면 오래된 `node_modules`가 원인일 수 있다.

```powershell
npm ci
npm test -- --watchAll=false
```

## 8. Vercel 프론트 실제 공개 배포

### 8.1 중요 구조

Vercel Root Directory 선택 화면에서 `frontend`가 나오지 않아 저장소 루트 배포 방식으로 변경했다. 루트 `vercel.json`이 다음 작업을 담당한다.

```text
Install: npm --prefix frontend ci
Build: npm --prefix frontend run build
Output: frontend/build
SPA rewrite: /(.*) → /index.html
```

따라서 Root Directory는 `./`를 선택한다.

### 8.2 Vercel 프로젝트 설정

```text
Root Directory: ./
Application/Framework Preset: Other
Install Command: npm --prefix frontend ci
Build Command: npm --prefix frontend run build
Output Directory: frontend/build
```

Build Command에 `start`를 넣으면 안 된다.

잘못된 예:

```text
npm start
npm --prefix frontend start
```

`start`는 종료되지 않는 개발 서버를 실행하므로 Vercel build가 완료되지 않는다.

### 8.3 Production Branch

현재 배포 코드는 `main`이 아니라 `depoly/test`에 있다.

```text
Vercel Project
→ Settings
→ Environments 또는 Git/Production 설정
→ Production
→ Branch Tracking
→ depoly/test
```

### 8.4 현재 발생한 No deployments 오류

오류:

```text
No deployments found for "depoly/test". Deploy the branch, then retry.
```

원인: Vercel 프로젝트가 생성되기 전에 `depoly/test`가 GitHub에 push되어 해당 프로젝트에 이 브랜치의 deployment 기록이 없다.

커밋 없이 해결하는 방법:

```text
Vercel Project
→ Deployments
→ Create Deployment
→ Git Reference/Branch에 depoly/test 입력
→ Deploy
```

배포 성공 후 Production의 Branch Tracking 화면으로 돌아가 `Retry` 또는 `Save`를 누른다.

### 8.5 `npm ci`와 package-lock 오류

오류 예시:

```text
The npm ci command can only install with an existing package-lock.json
```

로그에서 아래처럼 나온다면 저장소 루트에서 실행된 것이다.

```text
Branch: main
Running install command: npm ci
```

해결:

```text
Production Branch: depoly/test
Root Directory: ./
Install Command: npm --prefix frontend ci
Build Command: npm --prefix frontend run build
Output Directory: frontend/build
```

### 8.6 Vercel 환경변수

Render backend URL이 생성된 후 Vercel에 다음 값을 등록한다. 모두 `Config`로 등록한다.

```env
REACT_APP_API_BASE_URL=https://백엔드주소.onrender.com
REACT_APP_WS_URL=https://백엔드주소.onrender.com/ws
REACT_APP_TOSS_CLIENT_KEY=실제_Toss_클라이언트키
```

주의:

- API URL 끝에 `/api`를 붙이지 않는다. 애플리케이션 코드가 자동으로 붙인다.
- SockJS endpoint이므로 `REACT_APP_WS_URL`은 `wss://`가 아니라 `https://.../ws`를 사용한다.
- `REACT_APP_*`는 공개값이다. 서버 secret을 넣지 않는다.
- 환경변수 변경 후 반드시 Vercel을 Redeploy한다. CRA 환경변수는 build 시점에 포함된다.

## 9. Render Docker backend + PostgreSQL 실제 공개 배포

### 9.1 Blueprint 생성

Render Dashboard에서 진행한다.

```text
New
→ Blueprint
→ GitHub 저장소 LimHyeongHo/yuhan_final_project 연결
→ Branch: depoly/test
→ render.yaml 확인
```

Blueprint는 다음 자원을 생성한다.

```text
yuhan-final-project-api  Docker Web Service
yuhan-final-project-db   PostgreSQL
Region                   Singapore
```

### 9.2 Render backend 환경변수

`render.yaml`이 DB 연결정보와 일부 기본값을 자동 연결한다. `sync: false`로 표시된 값은 생성 화면에서 직접 입력해야 한다.

```env
FRONTEND_ORIGIN=https://yuhan-final-project-fc2q.vercel.app
BACKEND_PUBLIC_URL=https://실제-backend-이름.onrender.com
TOSS_SECRET_KEY=실제값
PORTONE_API_SECRET=실제값
NAVER_CLIENT_ID=실제값
NAVER_CLIENT_SECRET=실제값
ALADIN_TTB_KEY=실제값
KAKAO_REST_API_KEY=실제값
GOOGLE_BOOKS_API_KEY=실제값
BLOCKCHAIN_URL=새로발급한값
BLOCKCHAIN_PRIVATE_KEY=새로발급한값
BLOCKCHAIN_CONTRACT_ADDRESS=실제값
```

사용하지 않는 검색 API에는 임시로 `not-configured`를 입력할 수 있지만 해당 기능은 동작하지 않는다. Blockchain private key는 애플리케이션이 형식을 검사할 수 있으므로 올바른 형식의 새 키가 필요하다.

`PKI_SERVER_SECRET`은 Blueprint에서 무작위 값으로 자동 생성된다.

### 9.3 Render health check

배포가 완료되면 확인한다.

```text
https://실제-backend-주소.onrender.com/actuator/health/readiness
```

예상 응답:

```json
{"status":"UP"}
```

### 9.4 Render build가 Dockerfile을 찾지 못할 때

Blueprint를 사용하지 않고 Web Service를 수동 생성했다면 다음 값을 사용한다.

```text
Runtime: Docker
Branch: depoly/test
Dockerfile Path: ./backend/Dockerfile
Docker Build Context: ./backend
Health Check Path: /actuator/health/readiness
```

### 9.5 Render port 오류

애플리케이션은 `server.port=${PORT:8080}`을 사용한다. Render가 제공하는 `PORT`를 직접 덮어쓰지 않는다. 로그에 port binding 오류가 있으면 Render 환경변수에 잘못된 `PORT`가 수동 등록되어 있는지 확인한다.

### 9.6 DB 연결 오류

대표 오류:

```text
password authentication failed
Driver claims to not accept jdbcUrl
Unable to open JDBC Connection
```

Blueprint 사용 시 다음 값이 자동으로 만들어졌는지 확인한다.

```text
DATABASE_URL
DB_USERNAME
DB_PASSWORD
```

`DATABASE_URL`은 Render의 `postgresql://...` connection string이며 Spring 설정에서 `jdbc:` 접두사를 자동으로 붙인다. `DB_URL`을 별도로 수동 등록했다면 그 값이 우선하므로 잘못된 `DB_URL`을 제거하거나 올바른 JDBC URL로 수정한다.

## 10. Vercel과 Render 최종 연결 순서

서로의 URL이 필요하므로 다음 순서가 가장 안전하다.

1. Vercel frontend를 먼저 배포해 production URL을 확보한다.
2. Render Blueprint 생성 시 `FRONTEND_ORIGIN`에 Vercel URL을 입력한다.
3. Render backend 배포 후 backend URL을 확보한다.
4. Render의 `BACKEND_PUBLIC_URL`을 실제 backend URL로 확인/수정한다.
5. Vercel에 `REACT_APP_API_BASE_URL`, `REACT_APP_WS_URL`, `REACT_APP_TOSS_CLIENT_KEY`를 등록한다.
6. Vercel을 Redeploy한다.
7. frontend와 backend 양쪽을 통합 검증한다.

## 11. 배포 후 기능 검증

### 11.1 기본 확인

- [ ] Vercel 홈 화면이 열린다.
- [ ] `/login`, `/signup`, `/seller/dashboard` 직접 접속과 새로고침이 404가 아니다.
- [ ] 브라우저 개발자 도구 Network에 `localhost:8080` 요청이 없다.
- [ ] 요청 URL이 Render backend HTTPS 주소다.
- [ ] mixed-content 오류가 없다.
- [ ] backend readiness가 `UP`이다.

### 11.2 인증/CORS

- [ ] 회원가입과 로그인이 동작한다.
- [ ] 세션 쿠키가 전송된다.
- [ ] CORS 오류가 없다.
- [ ] CSRF token 발급과 상태변경 요청이 동작한다.
- [ ] 로그아웃이 동작한다.

### 11.3 주요 기능

- [ ] 상품 목록/상세/등록
- [ ] 이미지 업로드
- [ ] 채팅 WebSocket/SockJS 연결 및 재연결
- [ ] 결제 성공/실패 callback
- [ ] 관리자 페이지
- [ ] 외부 도서 검색 API
- [ ] Blockchain 검증 기능

## 12. 알려진 운영 제한

### 12.1 Render 무료 Web Service

- 15분 동안 inbound 요청이나 WebSocket message가 없으면 sleep될 수 있다.
- 다음 요청에서 다시 시작될 때 약 1분 정도 걸릴 수 있다.
- 로컬 filesystem은 재시작, 재배포, sleep 시 유지되지 않는다.

### 12.2 Render 무료 PostgreSQL

- 용량 제한이 있다.
- 생성 후 30일에 만료된다.
- 무료 DB에는 backup이 없다.
- 실제 장기 운영에는 유료 DB 또는 별도 관리형 PostgreSQL이 필요하다.

### 12.3 현재 이미지 업로드

현재 상품/채팅 이미지는 backend 로컬 `uploads/`에 저장한다. Render의 ephemeral filesystem에서는 파일이 사라질 수 있다. 발표용 단기 테스트는 가능하지만 실제 운영 전 S3 호환 object storage로 이전해야 한다.

### 12.4 세션과 WebSocket

현재 `HttpSession`과 simple WebSocket broker를 사용하므로 backend는 단일 instance 운영을 전제로 한다. 수평 확장 전 Redis session과 외부 message broker가 필요하다.

### 12.5 Scheduler

결제 환불/정산 scheduler는 backend instance마다 실행된다. 다중 instance 전환 전 분산 lock과 idempotency 보장이 필요하다.

## 13. 문제 발생 시 확인 순서

1. 어떤 단계인지 구분한다: GitHub / Vercel install / Vercel build / Render Docker build / Spring startup / DB / 브라우저 runtime.
2. 로그 첫 부분에서 branch와 commit SHA를 확인한다.
3. Vercel이면 Install, Build, Output Directory를 확인한다.
4. Render이면 Dockerfile path/context와 environment variables를 확인한다.
5. backend readiness endpoint를 직접 연다.
6. 브라우저 개발자 도구의 Console과 Network를 확인한다.
7. CORS 오류라면 `FRONTEND_ORIGIN`이 실제 Vercel production URL과 정확히 같은지 확인한다.
8. API가 localhost를 향하면 Vercel 환경변수 등록 후 Redeploy했는지 확인한다.
9. WebSocket이 실패하면 `REACT_APP_WS_URL=https://backend.onrender.com/ws`와 Render WebSocket 연결 로그를 확인한다.
10. DB가 실패하면 `DATABASE_URL`, `DB_USERNAME`, `DB_PASSWORD`를 확인한다.

## 14. 새 Codex 세션 시작 프롬프트

노트북에서 저장소를 열고 다음 문장을 새 Codex 세션에 전달한다.

```text
document/06_Deployment_Readiness.md와
document/07_DEPLOYMENT_HANDOFF.md를 전부 읽고
Docker + Vercel 실제 공개 배포 작업을 이어서 진행해줘.

현재 배포 브랜치는 depoly/test이고,
Vercel frontend 배포를 진행 중이며 Render backend는 아직 생성하지 않았다.

파일 수정 전 현재 상태를 먼저 확인하고,
커밋, push, merge, rebase는 반드시 내 허락을 받은 뒤 진행해.
비밀키 값은 출력하거나 Git에 추가하지 마.
```

## 15. 다음 작업

현재 바로 이어서 해야 할 작업은 다음과 같다.

1. Vercel `Deployments → Create Deployment`에서 `depoly/test`를 배포한다.
2. 성공한 deployment를 Production Branch에 연결한다.
3. Vercel production URL을 확인한다.
4. Render에서 `render.yaml` Blueprint를 생성한다.
5. Render secret 환경변수를 사용자가 직접 입력한다.
6. backend health check를 확인한다.
7. Render URL을 Vercel 환경변수에 등록하고 Redeploy한다.
8. 로그인, 세션, 상품, 이미지, 채팅, 결제를 검증한다.

