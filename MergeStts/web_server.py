import asyncio
import logging
import time
from contextlib import asynccontextmanager
from typing import Dict, Any

from fastapi import FastAPI, HTTPException, BackgroundTasks
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from models import (
    RecordRequest, TTSRequest, VoiceCommandRequest,
    VoiceResponse, HealthResponse, DurationPresetsResponse, ErrorResponse,
    BaseAudioRequest  # New import for Base64 audio
)
from services import STTService, TTSService
from config.settings import settings
from utils import ensure_temp_dir, cleanup_old_temp_files, format_processing_time

# 로깅 설정
logging.basicConfig(
    level=logging.INFO if settings.DEBUG else logging.WARNING,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# 전역 서비스 인스턴스
stt_service: STTService = None
tts_service: TTSService = None

@asynccontextmanager
async def lifespan(app: FastAPI):
    """애플리케이션 생명주기 관리"""
    # 시작 시 초기화
    global stt_service, tts_service
    
    logger.info("FastAPI 음성 서비스 시작 중...")
    
    try:
        # 임시 디렉토리 생성
        ensure_temp_dir(settings.TEMP_AUDIO_DIR)
        
        # 서비스 초기화
        logger.info("STT 서비스 초기화 중...")
        stt_service = STTService()
        
        logger.info("TTS 서비스 초기화 중...")
        tts_service = TTSService()
        
        # 오래된 임시 파일 정리
        cleanup_old_temp_files(settings.TEMP_AUDIO_DIR)
        
        logger.info("✅ 모든 서비스 초기화 완료")
        
    except Exception as e:
        logger.error(f"❌ 서비스 초기화 실패: {e}")
        raise
    
    yield
    
    # 종료 시 정리
    logger.info("FastAPI 음성 서비스 종료 중...")
    cleanup_old_temp_files(settings.TEMP_AUDIO_DIR)
    logger.info("✅ 서비스 정리 완료")

# FastAPI 앱 생성
app = FastAPI(
    title="MergeStts - 음성 서비스 API",
    description="Whisper STT + ElevenLabs TTS 통합 서비스",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# CORS 미들웨어 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.ALLOWED_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 예외 처리기
@app.exception_handler(Exception)
async def global_exception_handler(request, exc):
    """전역 예외 처리기"""
    logger.error(f"예상치 못한 오류: {exc}")
    return JSONResponse(
        status_code=500,
        content=ErrorResponse(
            error="internal_server_error",
            message="서버 내부 오류가 발생했습니다",
            details=str(exc) if settings.DEBUG else None
        ).dict()
    )

# 헬스 체크 엔드포인트
@app.get("/api/health", response_model=HealthResponse)
async def health_check():
    """서비스 상태 확인"""
    try:
        # 서비스 상태 확인
        stt_status = stt_service is not None and stt_service.model is not None
        tts_status = tts_service is not None and tts_service.client is not None
        
        return HealthResponse(
            status="healthy" if stt_status and tts_status else "degraded",
            service="MergeStts Voice API",
            version="1.0.0",
            docs_url="/docs",
            whisper_model=settings.WHISPER_MODEL if stt_service else "unknown"
        )
    except Exception as e:
        logger.error(f"헬스 체크 실패: {e}")
        raise HTTPException(status_code=503, detail="서비스 상태 확인 실패")

# Duration 프리셋 엔드포인트
@app.get("/api/duration-presets", response_model=DurationPresetsResponse)
async def get_duration_presets():
    """음성 녹음 시간 프리셋 반환"""
    return DurationPresetsResponse(
        presets=[
            {"name": "빠른 명령", "duration": 10.0, "description": "간단한 명령어용"},
            {"name": "일반 명령", "duration": 15.0, "description": "대부분의 명령어 (권장)"},
            {"name": "긴 명령", "duration": 30.0, "description": "복잡한 명령어나 긴 텍스트"}
        ],
        recommended=15.0,
        performance_info={
            "base_model": "whisper-base",
            "processing_time": {
                "15_seconds": "3-7초",
                "30_seconds": "5-10초"
            }
        }
    )

# STT 엔드포인트
@app.post("/api/record-and-transcribe", response_model=VoiceResponse)
async def record_and_transcribe(request: RecordRequest, background_tasks: BackgroundTasks):
    """마이크로 녹음하고 STT 수행"""
    if stt_service is None:
        raise HTTPException(status_code=503, detail="STT 서비스가 초기화되지 않았습니다")
    
    start_time = time.time()
    
    try:
        logger.info(f"STT 요청 - Duration: {request.duration}초")
        
        # 음성 녹음 및 변환
        text, audio_duration = await stt_service.record_and_transcribe(
            duration=request.duration
        )
        
        processing_time = time.time() - start_time
        
        # 백그라운드에서 임시 파일 정리
        background_tasks.add_task(
            cleanup_old_temp_files, 
            settings.TEMP_AUDIO_DIR,
            max_age_hours=1  # 1시간 후 정리
        )
        
        if text and text.strip():
            logger.info(f"STT 성공 - 텍스트: {text[:50]}...")
            return VoiceResponse(
                success=True,
                text=text.strip(),
                duration=audio_duration,
                processing_time=processing_time,
                message=f"음성 인식 완료 ({format_processing_time(processing_time)})"
            )
        else:
            logger.warning(f"STT 결과가 비어있음 - 원본 텍스트: '{text}', 오디오 길이: {audio_duration}초")
            return VoiceResponse(
                success=False,
                duration=audio_duration,
                processing_time=processing_time,
                error="음성을 인식할 수 없습니다. 마이크가 제대로 연결되어 있는지 확인하고 더 명확하게 말씀해주세요."
            )
            
    except Exception as e:
        processing_time = time.time() - start_time
        logger.error(f"STT 처리 실패: {e}")
        
        return VoiceResponse(
            success=False,
            duration=request.duration,
            processing_time=processing_time,
            error=f"음성 인식 실패: {str(e)}"
        )

# TTS 엔드포인트
@app.post("/api/text-to-speech", response_model=VoiceResponse)
async def text_to_speech(request: TTSRequest, background_tasks: BackgroundTasks):
    """텍스트를 음성으로 변환하고 재생"""
    if tts_service is None:
        raise HTTPException(status_code=503, detail="TTS 서비스가 초기화되지 않았습니다")
    
    start_time = time.time()
    
    try:
        logger.info(f"TTS 요청 - 텍스트: {request.text[:30]}...")
        
        # 텍스트를 음성으로 변환하고 재생
        success = await tts_service.speak(
            text=request.text,
            voice_id=request.voice_id
        )
        
        processing_time = time.time() - start_time
        
        # 백그라운드에서 임시 파일 정리
        background_tasks.add_task(
            cleanup_old_temp_files,
            settings.TEMP_AUDIO_DIR,
            max_age_hours=1
        )
        
        if success:
            logger.info("TTS 성공")
            return VoiceResponse(
                success=True,
                text=request.text,
                processing_time=processing_time,
                message=f"음성 재생 완료 ({format_processing_time(processing_time)})"
            )
        else:
            logger.warning("TTS 실패")
            return VoiceResponse(
                success=False,
                text=request.text,
                processing_time=processing_time,
                error="음성 재생에 실패했습니다"
            )
            
    except Exception as e:
        processing_time = time.time() - start_time
        logger.error(f"TTS 처리 실패: {e}")
        
        return VoiceResponse(
            success=False,
            text=request.text,
            processing_time=processing_time,
            error=f"음성 변환 실패: {str(e)}"
        )

# 통합 음성 명령 엔드포인트 (STT → 처리 → TTS)
@app.post("/api/voice-command", response_model=VoiceResponse)
async def voice_command(request: VoiceCommandRequest, background_tasks: BackgroundTasks):
    """음성 명령 통합 처리 (녹음 → STT → 응답 → TTS)"""
    if stt_service is None or tts_service is None:
        raise HTTPException(status_code=503, detail="음성 서비스가 초기화되지 않았습니다")
    
    start_time = time.time()
    
    try:
        logger.info(f"통합 음성 명령 요청 - Duration: {request.duration}초")
        
        # 1. 음성 녹음 및 STT
        text, audio_duration = await stt_service.record_and_transcribe(
            duration=request.duration
        )
        
        if not text or not text.strip():
            return VoiceResponse(
                success=False,
                duration=audio_duration,
                processing_time=time.time() - start_time,
                error="음성을 인식할 수 없습니다. 마이크가 제대로 연결되어 있는지 확인하고 더 명확하게 말씀해주세요."
            )
        
        # 2. 응답 생성 (여기서는 단순 에코, 실제로는 Node.js에서 처리)
        response_text = f"음성 명령을 받았습니다: {text}"
        
        # 3. TTS로 응답 재생
        if request.speak_response:
            await tts_service.speak(
                text=response_text,
                voice_id=request.voice_id
            )
        
        processing_time = time.time() - start_time
        
        # 백그라운드 정리
        background_tasks.add_task(
            cleanup_old_temp_files,
            settings.TEMP_AUDIO_DIR,
            max_age_hours=1
        )
        
        logger.info(f"통합 음성 명령 완료 - 인식된 텍스트: {text}")
        
        return VoiceResponse(
            success=True,
            text=text.strip(),
            duration=audio_duration,
            processing_time=processing_time,
            message=f"음성 명령 처리 완료 ({format_processing_time(processing_time)})"
        )
        
    except Exception as e:
        processing_time = time.time() - start_time
        logger.error(f"통합 음성 명령 실패: {e}")
        
        return VoiceResponse(
            success=False,
            duration=request.duration,
            processing_time=processing_time,
            error=f"음성 명령 처리 실패: {str(e)}"
        )

# 서비스 정보 엔드포인트
@app.get("/api/service-info")
async def get_service_info() -> Dict[str, Any]:
    """서비스 정보 반환"""
    try:
        stt_info = stt_service.get_service_info() if stt_service else {}
        tts_info = tts_service.get_service_info() if tts_service else {}
        
        return {
            "service_name": "MergeStts Voice API",
            "version": "1.0.0",
            "status": "running",
            "stt": stt_info,
            "tts": tts_info,
            "settings": {
                "default_duration": settings.DEFAULT_RECORD_DURATION,
                "min_duration": settings.MIN_RECORD_DURATION,
                "max_duration": settings.MAX_RECORD_DURATION,
                "whisper_model": settings.WHISPER_MODEL,
                "sample_rate": settings.SAMPLE_RATE
            }
        }
    except Exception as e:
        logger.error(f"서비스 정보 조회 실패: {e}")
        raise HTTPException(status_code=500, detail="서비스 정보 조회 실패")

# 음성 목록 엔드포인트
@app.get("/api/voices")
async def list_voices():
    """사용 가능한 TTS 음성 목록 반환"""
    if tts_service is None:
        raise HTTPException(status_code=503, detail="TTS 서비스가 초기화되지 않았습니다")
    
    try:
        voices = await tts_service.list_voices()
        return {
            "voices": voices,
            "default_voice_id": settings.ELEVENLABS_VOICE_ID
        }
    except Exception as e:
        logger.error(f"음성 목록 조회 실패: {e}")
        raise HTTPException(status_code=500, detail="음성 목록 조회 실패")

# 모바일 Base64 오디오 STT 엔드포인트
@app.post("/api/transcribe-base64", response_model=VoiceResponse)
async def transcribe_base64(request: BaseAudioRequest, background_tasks: BackgroundTasks):
    """모바일에서 전송한 Base64 인코딩된 오디오를 STT로 변환"""
    if stt_service is None:
        raise HTTPException(status_code=503, detail="STT 서비스가 초기화되지 않았습니다")
    
    start_time = time.time()
    
    try:
        logger.info(f"모바일 Base64 STT 요청 수신 - 데이터 길이: {len(request.audio_base64[:20])}...더 많은 문자")
        
        # Base64 오디오 처리 (새로운 메소드 사용)
        text, audio_duration = await stt_service.transcribe_base64(request.audio_base64)
        
        processing_time = time.time() - start_time
        
        # 백그라운드에서 임시 파일 정리
        background_tasks.add_task(
            cleanup_old_temp_files, 
            settings.TEMP_AUDIO_DIR,
            max_age_hours=1  # 1시간 후 정리
        )
        
        if text and text.strip():
            logger.info(f"모바일 Base64 STT 성공 - 텍스트: {text[:50]}...")
            return VoiceResponse(
                success=True,
                text=text.strip(),
                duration=audio_duration or request.duration,
                processing_time=processing_time,
                message=f"음성 인식 완료 ({format_processing_time(processing_time)})"
            )
        else:
            logger.warning("모바일 Base64 STT 결과가 비어있음")
            return VoiceResponse(
                success=False,
                duration=audio_duration or request.duration,
                processing_time=processing_time,
                error="음성을 인식할 수 없습니다. 오디오 품질을 확인하고 다시 시도해주세요."
            )
            
    except Exception as e:
        processing_time = time.time() - start_time
        logger.error(f"모바일 Base64 STT 처리 실패: {e}")
        
        return VoiceResponse(
            success=False,
            duration=request.duration,
            processing_time=processing_time,
            error=f"음성 인식 실패: {str(e)}"
        )

if __name__ == "__main__":
    import uvicorn
    
    # 개발 모드 실행
    uvicorn.run(
        "web_server:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=settings.DEBUG,
        log_level="info" if settings.DEBUG else "warning"
    )