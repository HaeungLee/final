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
    
    # CORS 설정
    ALLOWED_ORIGINS: list = [
        "http://localhost:5173",  # React 클라이언트
        "http://localhost:8081",  # Node.js API
        "http://localhost:3000",  # 개발용 추가 포트
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