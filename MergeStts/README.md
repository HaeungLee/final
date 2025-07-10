# MergeStts - FastAPI 기반 음성 서비스

**목적**: 기존 `/stt_tts` 테스트 코드를 기반으로 프로덕션 레디한 FastAPI 음성 서비스 구현

## 🏗️ 프로젝트 구조

```
/MergeStts/
├── requirements.txt        # FastAPI + STT/TTS 의존성
├── main.py                # FastAPI 앱 진입점
├── web_server.py          # 메인 서버 코드
├── models/                # Pydantic 모델들
│   ├── __init__.py
│   ├── requests.py        # 요청 모델들 (RecordRequest, TTSRequest)
│   └── responses.py       # 응답 모델들 (VoiceResponse)
├── services/              # 비즈니스 로직
│   ├── __init__.py
│   ├── stt_service.py     # Whisper STT 서비스
│   └── tts_service.py     # ElevenLabs TTS 서비스
├── utils/                 # 유틸리티 함수들
│   ├── __init__.py
│   └── audio_utils.py     # 오디오 처리 유틸
├── config/                # 설정 파일들
│   ├── __init__.py
│   └── settings.py        # 환경 설정
└── tests/                 # 테스트 파일들
    ├── __init__.py
    └── test_api.py        # API 테스트
```

## 🎯 주요 특징

- **FastAPI**: 비동기 처리, 자동 문서화, 타입 안전성
- **기존 코드 활용**: `/stt_tts/src` 로직 재사용
- **모듈화**: 깔끔한 서비스 분리
- **확장성**: nginx 독립 서비스화 준비

## 🚀 실행 방법

# Python 3.10 또는 3.11 설치 후
python3.10 -m venv venv_mergestts
# 또는
python3.11 -m venv venv_mergestts

# 활성화 후 설치
venv_mergestts\Scripts\activate  # Windows

### 1. 의존성 설치
```bash
cd MergeStts
pip install -r requirements.txt
```

### 2. 환경 설정 (선택사항)
```bash
# ElevenLabs API 키가 있다면
cp env.example .env
# .env 파일에서 ELEVENLABS_API_KEY 설정
```

### 3. 서버 실행
```bash
# 방법 1: Python 직접 실행
python main.py

# 방법 2: uvicorn 사용
uvicorn main:app --host 0.0.0.0 --port 8082 --reload

# 방법 3: 백그라운드 실행
nohup python main.py &
```

### 4. 테스트
```bash
# API 테스트 실행
pytest tests/ -v

# 헬스 체크
curl http://localhost:8082/api/health
```

## 📚 API 문서

- Swagger UI: `http://localhost:8082/docs`
- ReDoc: `http://localhost:8082/redoc`

## 📡 주요 API 엔드포인트

### 헬스 체크 & 정보
- `GET /api/health` - 서비스 상태 확인
- `GET /api/service-info` - 서비스 상세 정보
- `GET /api/duration-presets` - 녹음 시간 프리셋

### STT (음성 → 텍스트)
- `POST /api/record-and-transcribe` - 마이크 녹음 + STT
- `POST /api/voice-command` - 음성 명령 통합 처리

### TTS (텍스트 → 음성)
- `POST /api/text-to-speech` - 텍스트 음성 변환 + 재생
- `GET /api/voices` - 사용 가능한 음성 목록

### 요청 예시
```bash
# STT 테스트
curl -X POST "http://localhost:8082/api/record-and-transcribe" \
  -H "Content-Type: application/json" \
  -d '{"duration": 10.0}'

# TTS 테스트
curl -X POST "http://localhost:8082/api/text-to-speech" \
  -H "Content-Type: application/json" \
  -d '{"text": "안녕하세요, 테스트입니다"}'
```

## 🔗 연동 시스템

- **React Client** (5173) → **Node.js API** (8081) → **MergeStts** (8082)
- **Spring Security** (8080) ← **Node.js API** (8081) 
