# futureplan3.md - 최종 아키텍처 설계 및 구현 계획

## 🎯 목표
- **기존 Spring Security 백엔드(finalLogin/user)**와 **React 클라이언트(test2agent/client)**를 완전 연동
- **2개의 백엔드** 구조: Spring Security(인증) + Node.js(LLM Functions)
- **사용자별 OAuth 토큰 관리**로 .env 하드코딩 문제 해결
- **TDD, SOLID, Clean Architecture** 원칙 준수

## 🏗️ 최종 아키텍처

### 포트 구성
- **React Frontend**: `http://localhost:3000` (test2agent/client)
- **Spring Backend**: `http://localhost:8080` (finalLogin/user) - 인증 전담
- **Node.js Backend**: `http://localhost:8081` (test2agent/server) - LLM Functions 전담

### 데이터 플로우
```
사용자 → React(3000) → Spring(8080) → OAuth 인증 → JWT 발급
                    ↓
             Node.js(8081) ← JWT 검증 ← 토큰으로 API 호출
```

## 🔐 인증 플로우 세부 설계

### 1. OAuth 리다이렉트 처리
- **현재 설정**: `redirect-url: http://localhost:3000/oauth2/redirect`
- **React Router 구조**:
  ```
  / (메인 - 로그인 전 랜딩)
  /login (로그인 폼)
  /signup (회원가입 폼)
  /oauth2/redirect (OAuth 콜백 처리)
  /chat (인증 후 메인 채팅 - 기존 Chat 컴포넌트)
  /history (사이드바 - LLM Function 사용 내역)
  /usage (사이드바 - API 사용량 통계)
  ```

### 2. JWT 저장 방식
- **기존 Spring 구현 방식 그대로 사용**:
  - Access Token: 30분 (메모리/localStorage)
  - Refresh Token: 7일 (HttpOnly Cookie)
  - Redis 기반 토큰 관리

### 3. 토큰 자동 갱신 로직
```typescript
// 클라이언트 측 Axios Interceptor
axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      try {
        const newToken = await refreshAccessToken();
        // 원래 요청 재시도
        return axios.request({
          ...error.config,
          headers: { ...error.config.headers, Authorization: `Bearer ${newToken}` }
        });
      } catch (refreshError) {
        // 로그인 페이지로 리다이렉트
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
```

## 🎨 프론트엔드 아키텍처

### 1. 상태 관리: **Zustand** 선택
**추천 이유**:
- Redux Toolkit보다 **보일러플레이트 코드 적음**
- TypeScript 지원 우수
- 러닝 커브 낮음
- 번들 크기 작음 (2.9kb vs RTK 47kb)

```typescript
// stores/authStore.ts
interface AuthState {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  login: (credentials: LoginCredentials) => Promise<void>;
  logout: () => void;
  refreshToken: () => Promise<void>;
}

const useAuthStore = create<AuthState>((set, get) => ({
  user: null,
  accessToken: null,
  isAuthenticated: false,
  // ... 구현
}));
```

### 2. 라우팅 구조
```typescript
// App.tsx
<BrowserRouter>
  <Routes>
    <Route path="/" element={<Landing />} />
    <Route path="/login" element={<Login />} />
    <Route path="/signup" element={<Signup />} />
    <Route path="/oauth2/redirect" element={<OAuthCallback />} />
    
    {/* 인증 필요 라우트 */}
    <Route element={<ProtectedRoute />}>
      <Route path="/chat" element={<ChatLayout />}>
        <Route index element={<Chat />} />
        <Route path="history" element={<FunctionHistory />} />
        <Route path="usage" element={<UsageStatistics />} />
      </Route>
    </Route>
  </Routes>
</BrowserRouter>
```

### 3. UI 라이브러리: **Material-UI + Tailwind CSS**
```bash
npm install @mui/material @emotion/react @emotion/styled
npm install @mui/icons-material
npm install @tailwindcss/vite
```

### 4. 폴더 구조 개편
```
src/
├── components/
│   ├── auth/              # 로그인/회원가입 컴포넌트
│   │   ├── LoginForm.tsx
│   │   ├── SignupForm.tsx
│   │   └── OAuthButtons.tsx
│   ├── layout/            # 레이아웃 컴포넌트
│   │   ├── Header.tsx
│   │   ├── Sidebar.tsx
│   │   └── ProtectedRoute.tsx
│   ├── chat/              # 기존 채팅 컴포넌트 (유지)
│   └── common/            # 공통 컴포넌트
├── pages/                 # 페이지 컴포넌트
│   ├── Landing.tsx
│   ├── Login.tsx
│   ├── Signup.tsx
│   ├── OAuthCallback.tsx
│   └── ChatLayout.tsx
├── stores/                # Zustand 스토어
│   ├── authStore.ts
│   ├── chatStore.ts
│   └── uiStore.ts
├── services/              # API 호출 로직
│   ├── authService.ts
│   ├── userService.ts
│   └── agenticaService.ts
├── hooks/                 # 커스텀 훅
│   ├── useAuth.ts
│   ├── useApi.ts
│   └── useWebSocket.ts
├── utils/                 # 유틸리티
│   ├── axios.ts          # Axios 인터셉터 설정
│   ├── constants.ts
│   └── validators.ts
└── types/                 # TypeScript 타입 정의
    ├── auth.ts
    ├── user.ts
    └── agentica.ts
```

## 🔗 백엔드 통합 설계

### 1. 라우트 가드 구현
```typescript
// components/layout/ProtectedRoute.tsx
const ProtectedRoute = () => {
  const { isAuthenticated, user } = useAuthStore();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (user && !user.emailVerified) {
    return <EmailVerificationRequired />;
  }

  return <Outlet />;
};
```

### 2. RPC 호출 시 JWT 자동 포함
```typescript
// services/agenticaService.ts
class AgenticaService {
  private rpcClient: AgenticaRpcClient;

  constructor() {
    this.rpcClient = new AgenticaRpcClient({
      url: 'ws://localhost:8081',
      headers: () => ({
        Authorization: `Bearer ${useAuthStore.getState().accessToken}`
      })
    });
  }

  async callFunction(functionName: string, params: any) {
    // JWT가 자동으로 헤더에 포함됨
    return await this.rpcClient.call(functionName, params);
  }
}
```

### 3. Node.js 서버에서 JWT 검증 및 사용자별 토큰 조회
```typescript
// test2agent/server/src/middleware/auth.ts
export const authenticateJWT = async (req: Request, res: Response, next: NextFunction) => {
  const token = req.headers.authorization?.replace('Bearer ', '');
  
  if (!token) {
    return res.status(401).json({ error: 'Access token required' });
  }

  try {
    // Spring 백엔드에서 토큰 검증 및 사용자 정보 조회
    const userInfo = await axios.get(`http://localhost:8080/api/auth/verify`, {
      headers: { Authorization: `Bearer ${token}` }
    });

    // 사용자별 OAuth 토큰 조회
    const oauthTokens = await axios.get(`http://localhost:8080/api/user/oauth-tokens`, {
      headers: { Authorization: `Bearer ${token}` }
    });

    req.user = userInfo.data;
    req.oauthTokens = oauthTokens.data;
    next();
  } catch (error) {
    return res.status(401).json({ error: 'Invalid token' });
  }
};
```

## 🌐 CORS 및 환경 설정

### 1. Spring 백엔드 CORS 설정 수정
```yaml
# application-dev.yml
app:
  frontend:
    url: http://localhost:3000  # React 클라이언트
  backend:
    node-url: http://localhost:8081  # Node.js 백엔드
```

### 2. 환경별 동적 설정
```typescript
// utils/config.ts
const config = {
  development: {
    SPRING_API_URL: 'http://localhost:8080',
    NODE_API_URL: 'http://localhost:8081',
    WEBSOCKET_URL: 'ws://localhost:8081'
  },
  production: {
    SPRING_API_URL: process.env.REACT_APP_SPRING_API_URL,
    NODE_API_URL: process.env.REACT_APP_NODE_API_URL,
    WEBSOCKET_URL: process.env.REACT_APP_WEBSOCKET_URL
  }
};
```

## 📋 구현 단계별 계획

### Phase 1: 기본 인증 연동 (1주차)
1. **React Router 설정** 및 기본 페이지 구성
2. **Zustand 스토어** 설정 (인증 상태 관리)
3. **로그인/회원가입 폼** 구현 (Material-UI)
4. **Spring 백엔드 API 연동** (기존 API 활용)
5. **JWT 저장 및 자동 갱신** 로직 구현

### Phase 2: OAuth 소셜 로그인 (2주차)
1. **OAuth 버튼** 컴포넌트 구현
2. **OAuth 콜백 처리** 페이지 구성
3. **소셜 로그인 플로우** 테스트 및 검증

### Phase 3: Agentica 통합 (3주차)
1. **라우트 가드** 구현
2. **기존 Chat 컴포넌트** 인증 연동
3. **RPC 클라이언트** JWT 인터셉터 구현
4. **Node.js 서버** JWT 검증 미들웨어 구현

### Phase 4: 사용자별 토큰 관리 (4주차)
1. **Spring 백엔드** 사용자별 OAuth 토큰 저장/조회 API 구현
2. **Node.js 서버** 동적 토큰 주입 로직 구현
3. **Function 호출** 시 사용자별 토큰 활용
4. **.env 하드코딩 제거** 및 검증

### Phase 5: UI/UX 완성 (5주차)
1. **사이드바** 구현 (Function 내역, 사용량 통계)
2. **반응형 디자인** 적용
3. **로딩/에러 상태** 처리
4. **전체 테스트** 및 버그 수정

## ❓ 아직 명확하지 않은 점들

### 1. Spring 백엔드 API 확장
- **사용자별 OAuth 토큰 저장/조회 API**가 현재 구현되어 있나요?
  ```
  GET /api/user/oauth-tokens (사용자별 구글/네이버/카카오 토큰 조회)
  POST /api/user/oauth-tokens (토큰 저장/갱신)
  ```

### 2. Node.js 서버 포트 변경
- 현재 Node.js 서버가 어떤 포트에서 실행되고 있나요?
- **8081로 변경**해도 되나요?

### 3. 데이터베이스 스키마
- **OAuth 토큰 저장**을 위한 테이블 구조가 이미 있나요?
- 사용자별로 여러 소셜 계정 연동이 가능한 구조인가요?

### 4. 기존 Agentica Function 목록
- 현재 구현된 Function들이 어떤 외부 API를 사용하나요?
- **구글 API** 외에 다른 서비스들의 토큰 관리도 필요한가요?

### 5. 에러 처리 전략
- **토큰 만료/갱신 실패** 시 사용자 경험은 어떻게 설계할까요?
- **네트워크 오류** 시 재시도 로직이 필요한가요?

이 부분들을 명확히 하면 완벽한 구현 계획을 세울 수 있을 것 같습니다! 🚀 