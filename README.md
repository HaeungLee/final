[![Java](https://img.shields.io/badge/java-007396?style=flat&logo=java&logoColor=white)](https://www.oracle.com/java/)
[![Python](https://img.shields.io/badge/Python-3776AB?style=flat&logo=Python&logoColor=white)](https://www.python.org/)
[![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=flat&logo=javascript&logoColor=black)](https://developer.mozilla.org/en-US/docs/Web/JavaScript)
[![TypeScript](https://img.shields.io/badge/Typescript-3178C6?style=flat&logo=Typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Spring](https://img.shields.io/badge/Spring-6DB33F?style=flat&logo=Spring&logoColor=white)](https://spring.io/)
[![React](https://img.shields.io/badge/React-61DAFB?style=flat&logo=React&logoColor=black)](https://reactjs.org/)
[![React Native](https://img.shields.io/badge/React%20Native-61DAFB?style=flat&logo=React&logoColor=black)](https://reactnative.dev/)
[![Node.js](https://img.shields.io/badge/Node.js-339933?style=flat&logo=Node.js&logoColor=white)](https://nodejs.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=Docker&logoColor=white)](https://www.docker.com/)
[![Bootstrap](https://img.shields.io/badge/Bootstrap-7952B3?style=flat&logo=bootstrap&logoColor=white)](https://getbootstrap.com/)
[![GitHub](https://img.shields.io/badge/GitHub-181717?style=flat&logo=GitHub&logoColor=white)](https://github.com/)
[![PostgreSQL](https://img.shields.io/badge/postgresql-4169e1?style=flat&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=flat&logo=MongoDB&logoColor=white)](https://www.mongodb.com/)
[![HTML5](https://img.shields.io/badge/HTML5-E34F26?style=flat&logo=html5&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/HTML)
[![Tailwind CSS](https://img.shields.io/badge/TailwindCSS-38B2AC?style=flat&logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)
[![Docker Compose](https://img.shields.io/badge/Docker%20Compose-2496ED?style=flat&logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![NGINX](https://img.shields.io/badge/NGINX-009639?style=flat&logo=nginx&logoColor=white)](https://nginx.org/)
[![Spring Security](https://img.shields.io/badge/springsecurity-6DB33F?style=flat&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![FastAPI](https://img.shields.io/badge/fastapi-009688?style=flat&logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com/)
[![Hugging Face](https://img.shields.io/badge/huggingface-FFD21E?style=flat&logo=huggingface&logoColor=white)](https://huggingface.co/)
[![OpenAI](https://img.shields.io/badge/openai-000000?style=flat&logo=openai&logoColor=white)](https://openai.com/)
[![WebSocket](https://img.shields.io/badge/WebSocket-FABB00?style=flat&logo=websocket&logoColor=white)](https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API)
[![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat&logo=redis&logoColor=white)](https://redis.io/)
[![JWT](https://img.shields.io/badge/JWT-000000?style=flat&logo=json-web-tokens&logoColor=white)](https://jwt.io/)

# 🚀 AI Voice Assistant Platform (Final Project)

> **마이크로서비스 기반 AI 음성 비서 플랫폼**

## 📋 프로젝트 개요

이 프로젝트는 Agentica를 기반으로 한 마이크로서비스 아키텍처의 AI 음성 비서 플랫폼입니다.
Spring Security, Node.js, Python FastAPI, React를 활용한 종합적인 솔루션을 제공합니다.

## 🏗️ 아키텍처

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   React Client  │    │   Node.js API   │    │ Spring Security │
│   (Port 5173)   │◄──►│   (Port 8081)   │◄──►│   (Port 8080)   │
│                 │    │                 │    │                 │
│ • 음성 UI       │    │ • Agent Logic   │    │ • JWT Auth      │
│ • 채팅 인터페이스 │    │ • WebSocket     │    │ • OAuth2        │
│ • 실시간 상태    │    │ • Function Call │    │ • PostgreSQL    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐    ┌─────────────────┐
                       │  Voice Proxy    │    │ Python STT/TTS  │
                       │   (Port 8083)   │◄──►│   (Port 8082)   │
                       │                 │    │                 │
                       │ • CORS 처리     │     │ • FastAPI       │
                       │ • 타임아웃 관리  │     │ • Whisper STT   │
                       │ • 에러 핸들링    │     │ • ElevenLabs    │
                       └─────────────────┘    └─────────────────┘
```

## 🚀 빠른 시작

### 1. 의존성 설치

```bash
# Node.js 프로젝트들
cd finalteam/test2agent/server && npm install
cd finalteam/test2agent/client && npm install
cd finalteam/native && npm install

# Python 프로젝트
cd MergeStts && pip install -r requirements.txt
```

### 2. 모든 서버 실행

```bash
# 권장: 모든 개발 서버 동시 실행
.\run_all_dev.cmd

# 또는 개별 실행
.\quick_voice_proxy.cmd    # Voice Proxy 서버 (8083)
```

### 3. 서버 상태 확인

```bash
.\check_servers.cmd
```

## 📂 프로젝트 구조

```
├── finalLogin/          # Spring Security 인증 서버 (Port 8080)
│   └── user/           # JWT, OAuth2, PostgreSQL
├── finalteam/          # 메인 애플리케이션
│   ├── test2agent/
│   │   ├── server/     # Node.js Backend (Port 3000, 8081, 8083)
│   │   └── client/     # React Frontend (Port 5173)
│   └── native/         # React Native Mobile (Port 8084)
├── MergeStts/          # Python STT/TTS 서버 (Port 8082)
│   ├── services/       # Whisper STT, ElevenLabs TTS
│   └── web_server.py   # FastAPI 서버
└── 실행 스크립트들
    ├── run_all_dev.cmd      # 전체 개발 환경 실행
    ├── quick_voice_proxy.cmd # Voice Proxy만 실행
    └── check_servers.cmd    # 서버 상태 확인
```

## 🛠️ 기술 스택

### Frontend
- **React 19** + TypeScript
- **Tailwind CSS**
- **WebSocket** 실시간 통신
- **React Native** (Mobile)

### Backend
- **Spring Security** - JWT 인증, OAuth2
- **Node.js** + Agentica - AI Agent 로직
- **Python FastAPI** - STT/TTS 처리
- **Voice Proxy** - CORS 및 에러 핸들링

### Database & Infrastructure
- **PostgreSQL** - 사용자 데이터
- **Redis** - 토큰 캐시
- **Docker** - 컨테이너화
- **nginx** - 로드밸런싱

## 🔧 개발 가이드

### 환경 변수 설정

각 서비스별로 `.env` 파일을 설정해주세요:

```bash
# finalteam/test2agent/server/.env
OPENAI_API_KEY=your_openai_key
WEBSOCKET_PORT=8081

# MergeStts/.env
ELEVENLABS_API_KEY=your_elevenlabs_key
WHISPER_MODEL=base
```

### 포트 구성

| 서비스 | 포트 | 설명 |
|--------|------|------|
| Spring Security | 8080 | JWT 인증, OAuth2 |
| Python STT/TTS | 8082 | FastAPI, Whisper, ElevenLabs |
| Voice Proxy | 8083 | CORS 처리, 타임아웃 관리 |
| Node.js Backend | 3000, 8081 | Agent 로직, WebSocket |
| React Client | 5173 | 웹 클라이언트 |
| React Native | 8084 | 모바일 앱 |

### TDD 개발 방식

이 프로젝트는 TDD(Test-Driven Development) 방식으로 개발되었습니다:

```bash
# 테스트 실행
cd finalteam/test2agent/server && npm test
cd MergeStts && python -m pytest
```

## 🚨 문제 해결

### Voice Proxy 연결 오류
```bash
# Voice Proxy 서버 실행 확인
.\quick_voice_proxy.cmd

# 전체 서버 상태 확인
.\check_servers.cmd
```

### Metro/Expo 오류
```bash
.\quick_metro_fix.cmd      # 빠른 수정
.\complete_expo_fix.cmd    # 완전 복구
```

## 📚 문서

- [API 명세서](API_SPECIFICATION.md)
- [기능 요구사항](FUNCTIONAL_REQUIREMENTS.md)
- [Expo 성공 가이드](EXPO_SUCCESS_GUIDE.md)
- [프레젠테이션 자료](presentation.md)

## 🎯 주요 기능

- **음성 인터페이스**: STT/TTS를 통한 자연스러운 대화
- **AI Agent 시스템**: 다양한 서비스 통합 (Gmail, Calendar 등)
- **실시간 채팅**: WebSocket 기반 실시간 통신
- **모바일 지원**: React Native를 통한 크로스 플랫폼
- **보안**: JWT + OAuth2 기반 인증
- **마이크로서비스**: 독립적 확장 및 장애 격리

## 🤝 기여하기

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 라이선스

이 프로젝트는 MIT 라이선스 하에 배포됩니다. 자세한 내용은 `LICENSE` 파일을 참조하세요.

## 👥 팀

- **개발팀**: Full-Stack Development
- **기술 스택**: Spring Boot, Node.js, React, Python, Docker
- **아키텍처**: 마이크로서비스, Clean Architecture, SOLID 원칙

---

> **Note**: 이 프로젝트는 한국어에 최적화되어 있으며, TDD 방식과 Clean Architecture 원칙을 준수합니다.
>>>>>>> e2fa63a (feat: AI Voice Assistant Platform 통합)
