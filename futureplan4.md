# futureplan4.md - 최종 구현 계획서 (구체화)

## 🎯 확정된 목표
- **기존 Spring Security 백엔드(finalLogin/user)**와 **React 클라이언트(test2agent/client)**를 완전 연동
- **2개의 백엔드** 구조: Spring Security(8080, 인증) + Node.js(8081, LLM Functions)
- **사용자별 OAuth 토큰 관리**로 .env 하드코딩 문제 해결
- **구글, 카카오 OAuth**만 우선 구현 (네이버는 추후 고려)
- **TDD, SOLID, Clean Architecture** 원칙 준수

## 🏗️ 최종 아키텍처 (확정)

### 포트 구성
- **React Frontend**: `http://localhost:3000` (test2agent/client)
- **Spring Backend**: `http://localhost:8080` (finalLogin/user) - 인증 전담
- **Node.js Backend**: `http://localhost:8081` (test2agent/server) - LLM Functions 전담

### 데이터 플로우
```
사용자 → React(3000) → {
  Spring(8080) → OAuth 인증 → JWT 발급
  Node.js(8081) ← JWT 검증 ← 사용자별 토큰으로 API 호출
}
```

### 실제 API 호출 구조
```typescript
// React에서 직접 호출
const authAPI = axios.create({ baseURL: 'http://localhost:8080' });
const agentAPI = axios.create({ baseURL: 'http://localhost:8081' });

// 인증 관련
await authAPI.post('/api/auth/login', credentials);

// Agentica 관련  
await agentAPI.post('/api/agent/function', { function: 'gmail', params });
```

## 🔐 인증 플로우 세부 설계 (확정)

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

### 2. JWT 저장 방식 (기존 Spring 구현 활용)
- **Access Token**: 30분 (localStorage)
- **Refresh Token**: 7일 (HttpOnly Cookie)
- **Redis 기반** 토큰 관리

### 3. 토큰 자동 갱신 로직
```typescript
// utils/axios.ts - 클라이언트 측 Axios Interceptor
axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      try {
        const newToken = await refreshAccessToken();
        return axios.request({
          ...error.config,
          headers: { ...error.config.headers, Authorization: `Bearer ${newToken}` }
        });
      } catch (refreshError) {
        // 에러 상황 알림 후 홈으로 리다이렉트
        toast.error('로그인이 만료되었습니다. 다시 로그인해주세요.');
        useAuthStore.getState().logout();
        window.location.href = '/';
      }
    }
    return Promise.reject(error);
  }
);
```

### 4. 네트워크 오류 재시도 로직
```typescript
// utils/axios.ts - 2번 재시도
axios.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config;
    
    // 네트워크 오류 시 2번 재시도
    if (!config._retry && error.code === 'NETWORK_ERROR') {
      config._retry = (config._retry || 0) + 1;
      if (config._retry <= 2) {
        await new Promise(resolve => setTimeout(resolve, 1000 * config._retry));
        return axios.request(config);
      }
    }
    
    return Promise.reject(error);
  }
);
```

## 🎨 프론트엔드 아키텍처 (확정)

### 1. 상태 관리: **Zustand**
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
  accessToken: localStorage.getItem('accessToken'),
  isAuthenticated: !!localStorage.getItem('accessToken'),
  
  login: async (credentials) => {
    try {
      const response = await authService.login(credentials);
      localStorage.setItem('accessToken', response.accessToken);
      set({ 
        user: response.user, 
        accessToken: response.accessToken, 
        isAuthenticated: true 
      });
    } catch (error) {
      throw error;
    }
  },
  
  logout: () => {
    localStorage.removeItem('accessToken');
    set({ user: null, accessToken: null, isAuthenticated: false });
  },
  
  refreshToken: async () => {
    try {
      const response = await authService.refreshToken();
      localStorage.setItem('accessToken', response.accessToken);
      set({ accessToken: response.accessToken });
    } catch (error) {
      get().logout();
      throw error;
    }
  }
}));
```

### 2. UI 라이브러리: **Material-UI + Tailwind CSS**
```bash
npm install @mui/material @emotion/react @emotion/styled
npm install @mui/icons-material
npm install react-router-dom
npm install zustand
npm install axios
npm install react-hot-toast  # 에러 토스트용
```

### 3. 폴더 구조 (확정)
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
│       ├── LoadingSpinner.tsx
│       └── ErrorBoundary.tsx
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

## 🗄️ 데이터베이스 스키마 설계 (새로 구현)

### OAuth 토큰 저장 테이블 (PostgreSQL)
```sql
-- 사용자별 OAuth 토큰 저장 테이블
CREATE TABLE oauth_tokens (
    id BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    provider VARCHAR(50) NOT NULL,  -- 'google', 'kakao'
    access_token TEXT NOT NULL,
    refresh_token TEXT,
    expires_at TIMESTAMP,
    scope TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (member_id) REFERENCES member(id) ON DELETE CASCADE,
    UNIQUE(member_id, provider)  -- 사용자당 각 제공자별로 하나의 토큰만
);

-- 인덱스 추가
CREATE INDEX idx_oauth_tokens_member_provider ON oauth_tokens(member_id, provider);
CREATE INDEX idx_oauth_tokens_expires_at ON oauth_tokens(expires_at);
```

## 🔗 백엔드 통합 설계 (구체화)

### 1. Spring 백엔드 API 확장 (새로 구현)

#### 1.1 OAuth Token Entity
```java
// domain/oauth/OAuthToken.java
@Entity
@Table(name = "oauth_tokens")
public class OAuthToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OAuthProvider provider;
    
    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;
    
    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "scope", columnDefinition = "TEXT")
    private String scope;
    
    // ... 생성자, getter, setter, timestamps
}

// domain/oauth/OAuthProvider.java
public enum OAuthProvider {
    GOOGLE, KAKAO
}
```

#### 1.2 OAuth Token API
```java
// controller/OAuthTokenController.java
@RestController
@RequestMapping("/api/oauth-tokens")
@RequiredArgsConstructor
public class OAuthTokenController {
    
    private final OAuthTokenService oAuthTokenService;
    
    @GetMapping
    public ResponseEntity<Map<String, OAuthTokenDto>> getUserOAuthTokens(
            Authentication authentication) {
        String email = authentication.getName();
        Map<String, OAuthTokenDto> tokens = oAuthTokenService.getUserTokens(email);
        return ResponseEntity.ok(tokens);
    }
    
    @PostMapping
    public ResponseEntity<Void> saveOAuthToken(
            @RequestBody SaveOAuthTokenRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        oAuthTokenService.saveToken(email, request);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{provider}")
    public ResponseEntity<OAuthTokenDto> getProviderToken(
            @PathVariable String provider,
            Authentication authentication) {
        String email = authentication.getName();
        OAuthTokenDto token = oAuthTokenService.getProviderToken(email, provider);
        return ResponseEntity.ok(token);
    }
}
```

### 2. Node.js 서버 JWT 검증 미들웨어
```