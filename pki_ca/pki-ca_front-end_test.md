# PKI 통합 보안 시스템 실행 및 테스트 가이드

이 문서는 `pki_ca` 프로젝트를 팀원들이 로컬 환경에서 실행하고 프론트엔드를 통해 테스트하는 방법을 자세히 설명합니다.

## 📁 프로젝트 구조 요약
현재 프로젝트는 크게 3가지 컴포넌트로 구성되어 있습니다.
1. **`pki` (PKI Server)**
   - Spring Boot 4.x 기반 (Java 21)
   - MySQL 연동, 포트원(PortOne) 본인인증 API 사용
   - 포트: `8080`
2. **`ca` (Certificate Authority Server)**
   - Spring Boot 4.x 기반 (Java 21)
   - BouncyCastle 라이브러리를 사용한 인증서 발급/관리 (예상)
   - 포트: `8001`
3. **`front-end` (사용자 UI)**
   - 순수 HTML/JS/CSS (`index.html`)
   - IndexedDB 및 WebCrypto API를 활용한 브라우저 단의 키 페어 생성 및 서명
   - 포트원 본인인증 SDK 연동

---

## 🛠️ 사전 준비 사항 (Prerequisites)

1. **Java 21** 설치 완료 (JDK 21)
2. **MySQL** 서버 설치 및 실행 중 (기본 포트 3306)

---

## 🚀 실행 방법 (Step-by-Step)

### 1. 데이터베이스(MySQL) 설정
`pki` 서버는 MySQL을 사용합니다. 아래 정보에 맞게 데이터베이스를 준비합니다.
*   **DB 이름**: `project1`
*   **사용자명**: `root`
*   **비밀번호**: `123456`

MySQL에 접속하여 다음 명령어로 데이터베이스를 생성합니다.
```sql
CREATE DATABASE project1 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
*(💡 **참고**: Spring Boot JPA 설정(`ddl-auto=update`)에 의해 서버 최초 실행 시 **테이블과 컬럼은 자동으로 생성**됩니다. 따라서 빈 데이터베이스만 생성해 두시면 됩니다. 비밀번호가 다르다면 `pki/src/main/resources/application.properties` 파일의 `spring.datasource.password` 값을 수정하세요.)*

### 2. 포트원(PortOne) API 환경 변수 세팅
`pki` 서버는 포트원 API를 사용하기 위해 루트 폴더 내의 `.env` 파일을 로드합니다.
`pki/.env` 파일이 존재하는지 확인하고, 없다면 아래의 형식으로 생성해 주세요.
```env
# pki/.env 파일 내용
PORTONE_PUBLIC_STORE_ID=store-fd77dc69-cea9-4fd9-83c9-9888650fe579
PORTONE_PUBLIC_CHANNEL_KEY=channel-key-8ffdf987-acaa-40a7-bac8-c8000aab12f2
PORTONE_API_SECRET=pu2KdvJT2Pv8rpB5uP4nHrrUddMPDBAjrk3j8WHQd9ihro5Uk7xr1pBf8jkZ22le4Aj4K8vf56ub1FUW
```

### 3. 백엔드 서버 실행
터미널(또는 명령 프롬프트)을 두 개 열어 각각의 서버를 실행합니다.

**A. PKI 서버 실행**
```bash
cd pki
# Mac/Linux
./gradlew bootRun
# Windows
gradlew.bat bootRun
```
*   서버가 정상적으로 켜지면 `http://localhost:8080` 포트로 대기합니다.

**B. CA 서버 실행**
```bash
cd ca
# Mac/Linux
./gradlew bootRun
# Windows
gradlew.bat bootRun
```
*   서버가 정상적으로 켜지면 `http://localhost:8001` 포트로 대기합니다.

### 4. 프론트엔드 실행 및 테스트
별도의 웹 서버를 띄울 필요 없이, 탐색기(또는 Finder)에서 `front-end` 폴더로 이동합니다.
1. `index.html` 파일을 **더블 클릭**하여 웹 브라우저로 엽니다.
2. 브라우저 주소창에 `file:///.../index.html` 형태로 문서가 열리면 바로 테스트를 진행할 수 있습니다.

---

## 💻 기능 테스트 가이드 (How to Test)

브라우저에 접속하면 상단에 **현재 기기 식별자**(`dev-...` 형태의 UUID)가 보입니다. 
본 시스템은 기기 식별자와 연동되어 기기 단위의 보안이 이루어집니다.

### 테스트 시나리오 1: 회원가입 및 인증서 발급
1. **1단계: 통합 회원가입** 영역에서 이메일, 비밀번호, 닉네임, 회원 유형을 입력합니다.
2. `[본인인증 실행]` 버튼을 클릭합니다.
   * 포트원 모의 결제창(본인인증 창)이 뜹니다. 통신사 선택 후 인증을 완료해주세요. (테스트 모드이므로 실제 과금 등은 발생하지 않습니다.)
3. 인증이 완료되면 `[가입 및 인증서 발급]` 버튼이 활성화됩니다. 클릭합니다.
4. "인증서 발급 및 가입 성공!" 알림이 뜨면, 브라우저의 `IndexedDB`에 안전하게 개인키가 저장되고 가입 절차가 완료됩니다.

### 테스트 시나리오 2: 안전 로그인
1. **2단계: 보안 로그인** 영역에서 가입한 이메일과 비밀번호를 입력합니다.
2. `[본인인증 실행]` 버튼을 클릭하여 본인인증을 한 번 더 완료합니다.
3. 본인인증이 완료되면 나타나는 `[안전 로그인 실행]` 버튼을 클릭합니다.
4. 브라우저에 저장된 개인키로 서버가 내려준 챌린지(Challenge)에 전자서명을 수행하여 로그인됩니다.
5. "✨ [닉네임]님 환영합니다! ✨" 메시지가 나타납니다.

### 테스트 시나리오 3: 기기 인증서 재발급
*   **상황**: 브라우저 캐시를 지웠거나, 다른 브라우저/기기에서 접속하여 개인키(`IndexedDB`)가 유실된 경우 로그인이 불가능합니다.
*   **해결**: `[기기 인증서 재발급]` 버튼을 누르면 새로운 키 페어를 생성하고, 서버에 공개키를 갱신합니다. 이후 로그인이 가능해집니다.

### 테스트 시나리오 4: 기기 분실/인증서 폐기
1. 하단의 **기타: 기기 분실/인증서 폐기** 영역에서 `[현재 기기 인증서 무효화]` 버튼을 클릭합니다.
2. 브라우저에 저장된 개인키가 삭제되고, 서버 측에서도 해당 기기의 인증서 정보가 폐기됩니다.
3. 이후 기존 인증서로는 로그인이 불가능해집니다.

---

## 📝 문제 해결 (Troubleshooting)

1. **"Failed to fetch" 에러가 날 때**
   *   `pki` 서버(8080)가 정상적으로 실행 중인지 확인하세요.
   *   CORS 오류일 수 있으므로 `file://` 대신 `http://localhost...` 로 프론트엔드에 접속했는지 확인하세요.
2. **"기기 인증 정보가 없습니다" 에러가 날 때**
   *   해당 브라우저에 개인키가 발급되지 않았거나 삭제된 상태입니다. "기기 인증서 재발급" 버튼을 통해 새로운 인증서를 발급받으세요.
3. **DB 접속 에러가 날 때**
   *   MySQL 서비스가 켜져 있는지 확인하고, `application.properties`의 비밀번호가 본인의 로컬 MySQL 세팅과 맞는지 재확인하세요.
