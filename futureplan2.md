# futureplan2.md

## 1. 목표
- test2agent의 client와 user(Spring Security 기반 백엔드)를 연동하여 회원가입 및 OAuth(구글 등) 인증 후, 사용자별로 토큰을 받아와 LLM function 기능에 활용한다.
- 모든 코드는 TDD 방식으로 작성한다.
- SOLID 원칙 및 Clean Architecture를 준수한다.
- 한국어 데이터 및 지역 특성에 최적화한다.
- 최소한의 의존성으로 성능을 최적화한다.

## 2. 아키텍처 개요
1. **Client(프론트엔드)**
   - 회원가입/로그인 UI 제공 (Material UI, tailwindCSS 활용)
   - OAuth(구글 등) 로그인 버튼 제공
   - 회원가입/로그인 요청을 user 백엔드로 전송
2. **user(백엔드, Spring Security)**
   - 회원가입/로그인 API 제공 (JWT 등 토큰 발급)
   - OAuth 인증(구글 등) 처리 및 사용자별 access/refresh token 저장
   - 사용자별로 외부 API(구글 등) 토큰 관리
3. **server(LangChain/LLM 등)**
   - function 호출 시, user 백엔드에서 사용자별 토큰을 받아와 외부 API 호출에 사용

## 3. 주요 고려사항 및 구현 방법
### 1) client와 user 연동 (회원가입/로그인)
- client에서 회원가입/로그인 폼 구현 (Material UI, tailwindCSS 활용)
- 회원가입/로그인 요청을 user 백엔드로 전송 (REST API)
- 로그인 성공 시 JWT(혹은 세션) 저장, 이후 모든 요청에 인증 토큰 포함

### 2) OAuth(구글 등) 인증 및 토큰 관리
- client에서 "구글로 로그인" 버튼 → user 백엔드의 `/oauth2/authorization/google` 등으로 리다이렉트
- user 백엔드에서 OAuth 인증 후, 구글 access/refresh token을 DB에 저장(사용자별)
- user 백엔드가 JWT(혹은 세션) 발급 → client에 전달

### 3) function 호출 시 사용자별 토큰 활용
- client에서 function 호출 요청 시, JWT 포함하여 server로 전달
- server는 JWT를 user 백엔드에 전달하여 사용자별 구글 토큰 등 필요한 정보를 받아옴
- 받아온 토큰으로 구글 API 등 외부 서비스 호출

## 4. .env 하드코딩 문제 해결
- 기존에는 .env에 모든 토큰이 하드코딩되어 있었음
- 앞으로는 사용자별로 토큰을 user 백엔드에서 받아와 사용
- server에서는 .env에 공용 client_id, secret 등만 남기고, access_token 등은 요청 시마다 user 백엔드에서 받아옴

## 5. 예시 플로우
1. 사용자가 client에서 구글 로그인 → user 백엔드에서 인증 및 토큰 저장, JWT 발급
2. client가 function 호출 요청 시 JWT 포함
3. server가 user 백엔드에 JWT로 사용자 인증, 사용자별 구글 토큰 요청
4. server가 해당 토큰으로 구글 API 등 호출

## 6. API 설계 예시
- `POST /api/auth/signup` (회원가입)
- `POST /api/auth/login` (로그인)
- `GET /api/auth/oauth2/authorization/google` (구글 OAuth 시작)
- `GET /api/user/google-token` (server가 사용자별 구글 토큰 요청)

## 7. LLM function 연동
- function 호출 시, 필요한 외부 API 토큰을 user 백엔드에서 받아와 사용
- LLM function이 외부 API 호출 시, 사용자별 토큰을 동적으로 주입

## 8. 추가 구현 및 테스트 전략
- 모든 기능은 TDD 방식으로 테스트 코드부터 작성
- SOLID 원칙 및 Clean Architecture 구조로 모듈화
- 한국어 데이터 및 UX에 최적화된 설계
- 불필요한 외부 라이브러리 최소화, 성능 최적화

---

> 본 계획서는 프로젝트의 핵심 구조와 구현 전략을 요약한 문서입니다. 세부 구현 및 테스트 코드는 각 모듈별로 별도 작성합니다.
