# futureplan5-nginx.md - nginx 프록시 최종 아키텍처

## 🎯 수정된 목표 (nginx 프록시 방식)
- **기존 시스템 무변경**: Spring(8080), Node.js(3000) 그대로 유지
- **nginx 프록시**: 단일 엔드포인트로 백엔드 라우팅
- **CORS 문제 완전 해결**: 모든 요청이 같은 도메인
- **안전한 통합**: 기존 동작하는 시스템 보존

## 🏗️ 최종 아키텍처 (nginx 프록시)

### 포트 구성 (수정)
- **React Frontend**: `http://localhost:3000`
- **nginx Proxy**: `http://localhost:80` (개발용)
- **Spring Backend**: `http://localhost:8080` (nginx 뒤에 숨김)
- **Node.js Backend**: `http://localhost:3000` (nginx 뒤에 숨김, **포트 변경 불필요**)

### 데이터 플로우
```
사용자 → React(3000) → nginx(80) → {
  /api/auth/* → Spring(8080) [인증/OAuth]
  /api/agent/* → Node.js(3000) [Agentica Functions]
  /ws/* → Node.js(3000) [WebSocket]
  /* → React static files
}
```

## 🔧 nginx 설정

### nginx.conf
```nginx
events {
    worker_connections 1024;
}

http {
    upstream spring_backend {
        server localhost:8080;
    }
    
    upstream node_backend {
        server localhost:3000;
    }
    
    server {
        listen 80;
        server_name localhost;
        
        # React 정적 파일 서빙
        location / {
            proxy_pass http://localhost:3000;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
        
        # Spring 백엔드 (인증 관련)
        location /api/auth/ {
            proxy_pass http://spring_backend/api/auth/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
        
        # Spring 백엔드 (사용자 관련)
        location /api/member/ {
            proxy_pass http://spring_backend/api/member/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
        
        # Spring 백엔드 (OAuth 토큰 관련)
        location /api/oauth-tokens/ {
            proxy_pass http://spring_backend/api/oauth-tokens/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
        
        # Node.js 백엔드 (Agentica Functions)
        location /api/agent/ {
            proxy_pass http://node_backend/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }
        
        # WebSocket (Node.js)
        location /ws/ {
            proxy_pass http://node_backend;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }
    }
}
```

### Docker Compose (선택사항)
```yaml
# docker-compose.dev.yml
version: '3.8'
services:
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on:
      - spring-backend
      - node-backend
    
  spring-backend:
    # 기존 Spring 설정 그대로
    build: ./finalLogin/user
    ports:
      - "8080:8080"
    
  node-backend:
    # 기존 Node.js 설정 그대로  
    build: ./finalteam/test2agent/server
    ports:
      - "3000:3000"
    
  react-frontend:
    build: ./finalteam/test2agent/client
    ports:
      - "3000:3000"
```

## 🎨 React 변경사항 (최소한)

### API 호출 URL 변경
```typescript
// utils/config.ts (수정)
const config = {
  development: {
    API_BASE_URL: '',  // 상대경로로 변경
    WS_URL: 'ws://localhost/ws'  // nginx를 통한 WebSocket
  },
  production: {
    API_BASE_URL: '',  // 상대경로 유지
    WS_URL: 'wss://yourdomain.com/ws'
  }
};

// services/authService.ts (수정)
class AuthService {
  private baseURL = '/api/auth';  // nginx 라우팅 경로
  
  async login(credentials: LoginCredentials) {
    return axios.post(`${this.baseURL}/login`, credentials);
  }
  
  async refreshToken() {
    return axios.post(`${this.baseURL}/refresh`);
  }
}

// services/agenticaService.ts (수정)
class AgenticaService {
  private rpcClient = new AgenticaRpcClient({
    url: '/ws',  // nginx WebSocket 프록시
    headers: () => ({
      Authorization: `Bearer ${useAuthStore.getState().accessToken}`
    })
  });
  
  async callFunction(functionName: string, params: any) {
    // '/api/agent/' 경로로 HTTP 요청 시
    return axios.post('/api/agent/function', {
      function: functionName,
      params
    });
  }
}
```

## 📋 수정된 구현 계획

### Phase 1: nginx 프록시 설정 (1주차)
1. **nginx 설정 파일** 생성
2. **React API 호출 URL** 상대경로로 변경
3. **기본 라우팅 테스트** (Spring 연결 확인)
4. **WebSocket 프록시 테스트** (Node.js 연결 확인)
5. **기존 기능 동작 확인** (회귀 테스트)

### Phase 2: 인증 통합 (2주차)
1. **React Router 설정** 및 페이지 구성
2. **Zustand 스토어** 설정
3. **로그인/회원가입 폼** 구현
4. **기존 Spring API** 연동 테스트
5. **JWT 저장/갱신** 로직 구현

### Phase 3: OAuth 소셜 로그인 (3주차)
1. **OAuth 버튼** 컴포넌트 구현
2. **OAuth 콜백 처리** 페이지
3. **소셜 로그인 플로우** 테스트

### Phase 4: OAuth 토큰 저장 (4주차)
1. **Spring 백엔드** OAuth Token API 구현
2. **PostgreSQL 테이블** 생성
3. **토큰 저장/조회** 로직 구현

### Phase 5: Agentica 통합 (5주차)
1. **라우트 가드** 구현
2. **기존 Chat 컴포넌트** 인증 연동
3. **Node.js JWT 검증** 미들웨어
4. **사용자별 토큰 주입** 시스템

### Phase 6: UI/UX 완성 (6주차)
1. **사이드바** 구현
2. **반응형 디자인**
3. **에러 처리 및 테스트**

## ✅ 변경되지 않는 부분 (안전성 보장)

- **Spring 백엔드**: 포트, 설정, 코드, JWT 로직 **모두 그대로**
- **Node.js 백엔드**: 포트, 설정, 기존 Function 코드 **모두 그대로**
- **기존 OAuth 설정**: redirect URL 등 **모두 그대로**
- **데이터베이스**: 기존 스키마 **그대로**

## ✨ 추가 장점

1. **개발 편의성**: 하나의 도메인에서 모든 API 호출
2. **보안 강화**: 내부 포트 숨김, SSL 종료점 단일화
3. **모니터링**: nginx 로그로 모든 요청 추적 가능
4. **캐싱**: 정적 자원 캐싱으로 성능 향상
5. **확장성**: 로드밸런싱으로 확장 용이

이 방식으로 **기존 시스템의 안정성을 보장하면서** 새로운 기능을 안전하게 통합할 수 있습니다! 🚀 