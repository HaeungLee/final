from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from datetime import datetime

class VoiceResponse(BaseModel):
    """기본 음성 응답 모델"""
    success: bool = Field(..., description="작업 성공 여부")
    text: Optional[str] = Field(None, description="STT 결과 텍스트")
    duration: Optional[float] = Field(None, description="처리된 오디오 길이 (초)")
    processing_time: Optional[float] = Field(None, description="처리 시간 (초)")
    message: Optional[str] = Field(None, description="상태 메시지")
    error: Optional[str] = Field(None, description="오류 메시지")
    timestamp: datetime = Field(default_factory=datetime.now, description="응답 시간")
    
    class Config:
        json_schema_extra = {
            "example": {
                "success": True,
                "text": "안녕하세요, 음성 인식 테스트입니다.",
                "duration": 15.0,
                "processing_time": 3.2,
                "message": "STT 처리 완료",
                "timestamp": "2025-01-27T17:30:00"
            }
        }

class HealthResponse(BaseModel):
    """서버 상태 응답 모델"""
    status: str = Field(..., description="서버 상태")
    service: str = Field(..., description="서비스 이름")
    version: str = Field(default="1.0.0", description="서비스 버전")
    docs_url: str = Field(default="/docs", description="API 문서 URL")
    whisper_model: str = Field(..., description="사용 중인 Whisper 모델")
    
    class Config:
        json_schema_extra = {
            "example": {
                "status": "ok",
                "service": "MergeStts Voice Service",
                "version": "1.0.0",
                "docs_url": "/docs",
                "whisper_model": "base"
            }
        }

class DurationPreset(BaseModel):
    """녹음 시간 프리셋 모델"""
    name: str = Field(..., description="프리셋 이름")
    duration: float = Field(..., description="녹음 시간 (초)")
    description: str = Field(..., description="프리셋 설명")

class DurationPresetsResponse(BaseModel):
    """녹음 시간 프리셋 응답 모델"""
    presets: List[DurationPreset] = Field(..., description="사용 가능한 프리셋 목록")
    recommended: float = Field(..., description="권장 녹음 시간")
    performance_info: Dict[str, Any] = Field(..., description="성능 정보")
    
    class Config:
        json_schema_extra = {
            "example": {
                "presets": [
                    {"name": "빠른 명령", "duration": 10.0, "description": "간단한 명령어용"},
                    {"name": "일반 명령", "duration": 15.0, "description": "대부분의 명령어 (권장)"},
                    {"name": "긴 명령", "duration": 30.0, "description": "복잡한 명령어나 긴 텍스트"}
                ],
                "recommended": 15.0,
                "performance_info": {
                    "base_model": "whisper-base",
                    "processing_time": {
                        "15_seconds": "3-7초",
                        "30_seconds": "5-10초"
                    }
                }
            }
        }

class ErrorResponse(BaseModel):
    """오류 응답 모델"""
    success: bool = Field(default=False, description="작업 성공 여부")
    error: str = Field(..., description="오류 메시지")
    error_type: str = Field(..., description="오류 유형")
    timestamp: datetime = Field(default_factory=datetime.now, description="오류 발생 시간")
    
    class Config:
        json_schema_extra = {
            "example": {
                "success": False,
                "error": "마이크 접근에 실패했습니다.",
                "error_type": "AudioDeviceError",
                "timestamp": "2025-01-27T17:30:00"
            }
        } 