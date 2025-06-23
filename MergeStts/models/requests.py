from pydantic import BaseModel, Field
from typing import Optional

class RecordRequest(BaseModel):
    """음성 녹음 요청 모델"""
    duration: float = Field(
        default=15.0,
        ge=5.0,
        le=30.0,
        description="녹음 시간 (5-30초, 권장: 15초)"
    )
    
    class Config:
        json_schema_extra = {
            "example": {
                "duration": 15.0
            }
        }

class TTSRequest(BaseModel):
    """텍스트 음성 변환 요청 모델"""
    text: str = Field(
        ...,
        min_length=1,
        max_length=1000,
        description="음성으로 변환할 텍스트"
    )
    voice_id: Optional[str] = Field(
        default=None,
        description="ElevenLabs 음성 ID (선택사항)"
    )
    
    class Config:
        json_schema_extra = {
            "example": {
                "text": "안녕하세요! 음성 테스트입니다.",
                "voice_id": "21m00Tcm4TlvDq8ikWAM"
            }
        }

class VoiceCommandRequest(BaseModel):
    """음성 명령 요청 모델 (녹음 + STT)"""
    duration: float = Field(
        default=15.0,
        ge=5.0,
        le=30.0,
        description="녹음 시간 (5-30초)"
    )
    speak_response: bool = Field(
        default=False,
        description="응답을 음성으로 재생할지 여부"
    )
    voice_id: Optional[str] = Field(
        default=None,
        description="ElevenLabs 음성 ID (선택사항)"
    )
    
    class Config:
        json_schema_extra = {
            "example": {
                "duration": 15.0,
                "speak_response": False,
                "voice_id": "21m00Tcm4TlvDq8ikWAM"
            }
        } 