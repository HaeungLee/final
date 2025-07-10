from pydantic_settings import BaseSettings
from typing import Optional
import os

class Settings(BaseSettings):
    """애플리케이션 설정"""
    
    # 서버 설정
    HOST: str = "0.0.0.0"
    PORT: int = 8082
    DEBUG: bool = True
    
    # STT 설정 (Whisper)
    WHISPER_MODEL: str = "base"  # tiny, base, small, medium, large
    DEFAULT_RECORD_DURATION: float = 15.0
    MIN_RECORD_DURATION: float = 5.0
    MAX_RECORD_DURATION: float = 30.0
    SAMPLE_RATE: int = 16000
    
    # TTS 설정 (ElevenLabs)
    ELEVENLABS_API_KEY: Optional[str] = None
    ELEVENLABS_VOICE_ID: str = "21m00Tcm4TlvDq8ikWAM"  # Rachel 기본 음성
    
    # 오디오 설정
    AUDIO_FORMAT: str = "wav"
    TEMP_AUDIO_DIR: str = "./temp_audio"
    
    # CORS 설정 - 모바일 앱 접근 확장
    ALLOWED_ORIGINS: list = [
        # 웹 클라이언트
        "http://localhost:5173",
        "http://localhost:3000",
        # 노드 서버
        "http://localhost:8081",  # WebSocket
        "http://localhost:9081",  # HTTP API
        # Expo 모바일 앱 (다양한 환경)
        "http://localhost:8084",  # Expo 개발 서버 기본
        "http://localhost:19000",  # Expo 대체 포트
        "http://localhost:19006",  # Expo 웹 빌드
        # Android 에뮬레이터
        "http://10.0.2.2:8084",
        "http://10.0.2.2:19000",
        "http://10.0.2.2:19006",
        # iOS 시뮬레이터
        "http://127.0.0.1:8084",
        "http://127.0.0.1:19000",
        "http://127.0.0.1:19006",
        # 일반적인 로컬 네트워크 IP (개발 기기에 맞게 조정 필요)
        "http://192.168.0.92:8084",
        "http://192.168.0.92:19000",
        "http://192.168.0.92:19006",
        "http://192.168.0.92:9081",
        # 앱 스키마 허용
        "exp://localhost:8084",
        "exp://192.168.0.92:8084",
        # null origin (일부 React Native Webview)
        "null"
    ]
    
    # 로깅 설정
    LOG_LEVEL: str = "INFO"
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

# 전역 설정 인스턴스
settings = Settings()

# 임시 오디오 디렉토리 생성
os.makedirs(settings.TEMP_AUDIO_DIR, exist_ok=True)