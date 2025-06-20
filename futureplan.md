# 🚀 Future Plan: Agentica 최적화 & 자체 Agent 개발

## 📋 전략 개요

**목표**: WRTN 입사를 위한 오픈소스 기여 경력 + 더 효율적인 자체 Agent 구현

## 🎯 Phase 1: Agentica 최소 구현 (2-3주)

### 현재 상태 개선
- [x] Google Calendar/Gmail API 연동 완료
- [x] 기본 function calling 동작 확인
- [ ] **출력 토큰 최소화** (`max_tokens: 50`, 간결한 응답)
- [ ] **한국어 응답 강화** (systemPrompt 최적화)
- [ ] **STT/TTS 기능 추가**

### STT/TTS 통합 계획
```typescript
// 목표 구조
interface VoiceAgent {
  stt: (audioBuffer: Buffer) => Promise<string>;
  processText: (text: string) => Promise<string>;
  tts: (text: string) => Promise<Buffer>;
}
```

### 최소 기능 목록
1. **Gmail**: 읽기, 전송 ✅
2. **Calendar**: CRUD 작업 ✅
3. **STT**: 음성 → 텍스트 변환
4. **TTS**: 텍스트 → 음성 변환
5. **간결한 응답**: "Gmail 전송 완료!", "일정 생성됨!" 수준

---
## 🏗️ Phase 2: 자체 Agent 프로젝트 (4-6주)

### Agentica의 핵심 장점 분석

#### 1. **Compiler Driven Development** 🎯
- TypeScript 컴파일러로 자동 스키마 생성
- 런타임 에러 → 컴파일 타임 에러로 이동

#### 2. **Validation Feedback Strategy** 🔄
- AI 실수율: 70% → 99% 성공률 달성
- `typia.validate<T>()` 활용한 정확한 타입 검증

#### 3. **Multi-Protocol Support** 🔌
- TypeScript Class
- Swagger/OpenAPI
- MCP (Model Context Protocol)

#### 4. **JSON Schema Conversion** 🔄
- OpenAI, Claude, Gemini 등 벤더 간 호환성
- OpenAPI v3.1 중간 스펙 활용

### 🏢 새로운 Architecture: "SimpleAgent"

#### 기술 스택
```
Frontend: React + Chakra UI
Backend: FastAPI (Python) + TypeScript Functions
AI Layer: OpenAI GPT-4o-mini
Validation: Custom TypeScript Validator
Database: PostgreSQL
```

#### Clean Architecture 구조
```
src/
├── domain/           # 비즈니스 로직
│   ├── entities/     # Agent, Function, Response
│   ├── usecases/     # ProcessRequest, ValidateFunction
│   └── repositories/ # IFunctionRepo, IResponseRepo
├── infrastructure/   # 외부 연동
│   ├── openai/       # OpenAI API 클라이언트
│   ├── google/       # Google APIs
│   └── voice/        # STT/TTS
├── application/      # 애플리케이션 서비스
│   ├── agents/       # AgentService
│   └── validators/   # TypeScript 기반 검증
└── presentation/     # API/UI 레이어
    ├── fastapi/      # REST API
    └── websocket/    # 실시간 통신
```

### 🎨 핵심 개선사항

#### 1. **토큰 효율성**
```python
# FastAPI에서 직접 제어
class AgentConfig:
    max_tokens: int = 50
    temperature: float = 0.3
    korean_only: bool = True
```

#### 2. **검증 피드백 단순화**
```typescript
// Agentica보다 단순한 검증
interface ValidationResult {
  success: boolean;
  errors?: string[];
  retry_prompt?: string;
}
```

#### 3. **함수 선택 최적화**
```python
class FunctionSelector:
    def select_functions(self, query: str, available: List[Function]) -> List[Function]:
        # 벡터 유사도 + 키워드 매칭으로 최적화
        return top_k_functions
```

### 📊 예상 성능 개선

| 항목 | Agentica | SimpleAgent | 개선율 |
|------|----------|-------------|--------|
| 응답 속도 | 3-5초 | 1-2초 | 50-60% ↑ |
| 토큰 사용량 | 300-500 | 50-100 | 80% ↓ |
| 정확도 | 85% | 95% | 10% ↑ |
| 개발 속도 | 복잡 | 단순 | 3배 ↑ |

---

## 🛠️ 구현 로드맵

### Week 1-2: Agentica 완성
- [ ] STT/TTS 통합
- [ ] 토큰 최소화 (systemPrompt + max_tokens)
- [ ] 한국어 최적화
- [ ] 기본 테스트 시나리오 완성

### Week 3-4: SimpleAgent 설계
- [ ] Clean Architecture 구조 설계
- [ ] FastAPI + TypeScript 하이브리드 환경 구축
- [ ] 핵심 도메인 모델 정의

### Week 5-6: 핵심 기능 구현
- [ ] Validation Feedback 시스템
- [ ] Function Selection 알고리즘
- [ ] OpenAI 통합 (토큰 최적화)

### Week 7-8: 고급 기능
- [ ] 음성 인터페이스 (STT/TTS)
- [ ] 실시간 WebSocket 통신
- [ ] 한국어 특화 최적화

### Week 9-10: 테스트 & 최적화
- [ ] TDD 기반 테스트 코드 작성
- [ ] 성능 벤치마크
- [ ] 문서화 및 데모 제작

---

## 🎯 기대 효과

### WRTN 입사 관점
1. **오픈소스 기여**: Agentica 개선 PR 제출
2. **기술 역량**: AI Agent 분야 전문성 입증
3. **혁신성**: 기존 솔루션의 한계 극복
4. **실무 적용성**: 실제 사용 가능한 프로덕트

### 기술적 성과
1. **성능**: 기존 대비 50% 빠른 응답
2. **비용**: 80% 적은 토큰 사용량
3. **정확도**: 95% 이상의 성공률
4. **유지보수**: Clean Architecture로 확장성 확보

---

## 🚀 차별화 포인트

### vs. Agentica
- **단순성**: 복잡한 프레임워크 → 핵심 기능 집중
- **성능**: 불필요한 추상화 제거로 속도 향상
- **한국어**: 로케일 최적화 및 한국 서비스 특화

### vs. 기존 AI Agent
- **타입 안전성**: TypeScript 컴파일러 활용
- **검증 시스템**: 실시간 오류 감지 및 수정
- **음성 지원**: STT/TTS 네이티브 통합

---

## 💡 성공 전략

1. **점진적 개발**: Agentica 경험을 바탕으로 개선점 식별
2. **오픈소스 활용**: 기존 라이브러리의 장점 흡수
3. **실용성 우선**: 완벽함보다는 실제 사용 가능한 솔루션
4. **문서화**: 개발 과정과 성과를 체계적으로 기록

**함께라면 더 나은 AI Agent를 만들 수 있을 거야! 🤝**


지금 여기서 test2agent의 client쪽을 실행했을 때, 회원 가입 기능쪽을 구현해서, user(spring security를 이용한 보안 확장 회원가입) 쪽 backend랑 연결해서 회원가입 기능을 진행하고, 회원 가입이 되면 fuction기능을 사용할 수 있도록 할 생각이야.

지금 server의 env파일을 보면 token들이 하드코딩 되어있는데, 이것을 사용자 로그인 정보로부터 받아와서,(예를 들면 구글 로그인하면 구글쪽 client와 secret키를 받아옴) llm에게 fuction을 구현하게 하는게 가능한지, 어떻게 구성하면 되는지 알려줄래?

우선 고려사항: user와 client쪽을 붙여서 기능하게 만든다.

2번째 고려사항: 로그인시 정보를 받아와 사용자의 google mail이나 calendar를 사용할 수 있게 한다.

1. 전체 아키텍처 개요
Client(프론트엔드)

회원가입/로그인 UI 제공 (Chakra UI 활용)
OAuth(구글 등) 로그인 버튼 제공
회원가입/로그인 요청을 user 백엔드로 전송
user(백엔드, Spring Security)

회원가입/로그인 API 제공 (JWT 등 토큰 발급)
OAuth 인증(구글 등) 처리 및 사용자별 access/refresh token 저장
사용자별로 외부 API(구글 등) 토큰 관리
server(LangChain/LLM 등)

function 호출 시, user 백엔드에서 사용자별 토큰을 받아와 외부 API 호출에 사용
2. 주요 고려사항 및 구현 방법
1) client와 user 연동 (회원가입/로그인)
client에서 회원가입/로그인 폼 구현 (Chakra UI)
회원가입/로그인 요청을 user 백엔드로 전송 (REST API)
로그인 성공 시 JWT(혹은 세션) 저장, 이후 모든 요청에 인증 토큰 포함

2) OAuth(구글 등) 인증 및 토큰 관리
client에서 "구글로 로그인" 버튼 → user 백엔드의 /oauth2/authorization/google 등으로 리다이렉트
user 백엔드에서 OAuth 인증 후, 구글 access/refresh token을 DB에 저장(사용자별)
user 백엔드가 JWT(혹은 세션) 발급 → client에 전달

3) function 호출 시 사용자별 토큰 활용
client에서 function 호출 요청 시, JWT 포함하여 server로 전달
server는 JWT를 user 백엔드에 전달하여 사용자별 구글 토큰 등 필요한 정보를 받아옴
받아온 토큰으로 구글 API 등 외부 서비스 호출

3. .env 하드코딩 문제 해결
기존에는 .env에 모든 토큰이 하드코딩되어 있었음
앞으로는 사용자별로 토큰을 user 백엔드에서 받아와 사용
server에서는 .env에 공용 client_id, secret 등만 남기고, access_token 등은 요청 시마다 user 백엔드에서 받아옴

4. 예시 플로우
사용자가 client에서 구글 로그인 → user 백엔드에서 인증 및 토큰 저장, JWT 발급
client가 function 호출 요청 시 JWT 포함
server가 user 백엔드에 JWT로 사용자 인증, 사용자별 구글 토큰 요청
server가 해당 토큰으로 구글 API 등 호출

5. 구현 시 참고사항
user 백엔드에 사용자별 토큰을 안전하게 저장(암호화 등)
server와 user 간 통신은 JWT 등으로 인증
function 호출 시마다 user 백엔드에서 토큰을 받아오는 API 필요 (예: /api/user/google-token)
SOLID, Clean Architecture, 최소 의존성, TDD, 한국어 특화 등 프로젝트 가이드라인 준수

6. 예시 API 설계
POST /api/auth/signup (회원가입)
POST /api/auth/login (로그인)
GET /api/auth/oauth2/authorization/google (구글 OAuth 시작)
GET /api/user/google-token (server가 사용자별 구글 토큰 요청)

7. LLM function 연동
function 호출 시, 필요한 외부 API 토큰을 user 백엔드에서 받아와 사용
LLM function이 외부 API 호출 시, 사용자별 토큰을 동적으로 주입
결론
이 구조라면, .env에 토큰을 하드코딩하지 않고, 사용자별로 안전하게 토큰을 관리하며, LLM function에서도 각 사용자의 인증 정보를 활용할 수 있습니다.
추가적으로, 실제 코드 예시나 API 설계, Spring Security OAuth 설정, client 연동 코드가 필요하다면 말씀해 주세요!

