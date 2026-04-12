# 📚 AI 도서 검색 트렌드 분석


---

## 🛠 기술 스택
- Spring Boot
- Java 17
- WebFlux (OpenAI API 호출)
- OpenAI GPT-3.5-turbo

---

## ✅ 구현 기능

### 1. 검색어 저장
- 사용자가 도서 검색할 때마다 검색어와 횟수 저장
- 메모리 기반 저장 (ConcurrentHashMap)

### 2. 인기 검색어 조회
- 검색 횟수 기준 상위 10개 정렬
- 많이 검색된 순서대로 반환

### 3. AI 트렌드 분석 (핵심 기능)
- 검색 데이터를 OpenAI GPT-3.5-turbo에 전달
- 인기 급상승 키워드 top3 분석
- 검색 트렌드 기반 도서 3권 추천
- 트렌드 요약 한 줄 제공

### 4. 가독성 있는 텍스트 출력
- JSON이 아닌 텍스트 형식으로 보기 좋게 출력
- 이모지와 구분선으로 가독성 향상

---

## 📋 API 목록

| Method | URL | 설명 |
|--------|-----|------|
| POST | /api/search/log | 검색어 저장 |
| GET | /api/search/top | 상위 10개 검색어 조회 |
| GET | /api/search/trend | AI 트렌드 분석 (JSON) |
| GET | /api/search/trend/view | AI 트렌드 분석 (텍스트) |
| GET | /api/search/all | 전체 검색 현황 |

---

## ⚙️ 실행 방법

### 1. application.yml 생성

src/main/resources/application.yml 파일 생성 후 아래 내용 붙여넣기

```yaml
spring:
  application:
    name: ai-search-trend

server:
  port: 8081

openai:
  api-key: 본인_OpenAI_API_키_입력
  model: gpt-3.5-turbo
```

### 2. OpenAI API 키 발급
```
https://platform.openai.com 접속
→ 로그인
→ API Keys
→ Create new secret key
→ 복사해서 application.yml에 입력
```

### 3. 서버 실행
```
AiSearchTrendApplication.java 실행
```

---

## 🧪 테스트 방법

### 1. 검색어 저장 (Postman)
```
POST http://localhost:8081/api/search/log?keyword=자바
POST http://localhost:8081/api/search/log?keyword=자바
POST http://localhost:8081/api/search/log?keyword=스프링
POST http://localhost:8081/api/search/log?keyword=파이썬
POST http://localhost:8081/api/search/log?keyword=알고리즘
```
![트렌드 분석 결과](images/postman.png)

### 2. 인기 검색어 확인
```
GET http://localhost:8081/api/search/top
```

### 3. AI 트렌드 분석 (텍스트)
```
GET http://localhost:8081/api/search/trend/view
```

### 4. 예상 결과

![트렌드 분석 결과](images/answer_result.png)

---

## ⚠️ 주의사항
- application.yml은 보안상 깃허브에 올리지 않습니다
- OpenAI API 키는 유료이므로 본인 키를 직접 발급받아야 합니다
- 현재 메모리 기반 저장이라 서버 재시작 시 데이터 초기화됩니다 (DB 연동 예정)

---

## 💡 기존 방식과 차이점

| | 단순 DB 집계 | AI 활용 |
|---|---|---|
| 인기 검색어 순위 | ✅ 가능 | ✅ 가능 |
| 트렌드 분석 | ❌ 불가 | ✅ 가능 |
| 도서 추천 | ❌ 불가 | ✅ 가능 |
| 트렌드 이유 설명 | ❌ 불가 | ✅ 가능 |




