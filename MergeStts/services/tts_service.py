import os
import asyncio
import logging
from typing import Optional, List, Dict, Any
from elevenlabs import play, stream
from elevenlabs.client import ElevenLabs
import tempfile

from config.settings import settings

logger = logging.getLogger(__name__)

class TTSService:
    """ElevenLabs 기반 텍스트 음성 변환 서비스"""
    
    def __init__(self):
        """TTS 서비스 초기화"""
        self.client = None
        self.voice_id = settings.ELEVENLABS_VOICE_ID
        self._initialize_client()
    
    def _initialize_client(self):
        """ElevenLabs 클라이언트 초기화"""
        api_key = settings.ELEVENLABS_API_KEY
        if not api_key:
            logger.warning("ElevenLabs API 키가 설정되지 않았습니다")
            return
        
        try:
            self.client = ElevenLabs(api_key=api_key)
            logger.info("ElevenLabs 클라이언트 초기화 완료")
        except Exception as e:
            logger.error(f"ElevenLabs 클라이언트 초기화 실패: {e}")
            raise RuntimeError(f"TTS 서비스 초기화 실패: {e}")
    
    def _check_client(self):
        """클라이언트 사용 가능 여부 확인"""
        if self.client is None:
            raise RuntimeError("ElevenLabs API 키가 설정되지 않았습니다")
    
    async def list_voices(self) -> List[Dict[str, str]]:
        """
        사용 가능한 음성 목록 조회 (비동기)
        
        Returns:
            List[Dict[str, str]]: 음성 ID와 이름 목록
        """
        self._check_client()
        
        try:
            loop = asyncio.get_event_loop()
            voices = await loop.run_in_executor(
                None,
                self._list_voices_sync
            )
            return voices
        except Exception as e:
            logger.error(f"음성 목록 조회 실패: {e}")
            return []
    
    def _list_voices_sync(self) -> List[Dict[str, str]]:
        """동기 음성 목록 조회 (내부 사용)"""
        try:
            all_voices = self.client.voices.get_all()
            return [
                {"id": v.voice_id, "name": v.name} 
                for v in all_voices.voices
            ]
        except Exception as e:
            logger.error(f"음성 목록 조회 중 오류: {e}")
            return []
    
    def get_available_models(self) -> List[Dict[str, Any]]:
        """
        사용 가능한 TTS 모델 목록 반환
        
        Returns:
            List[Dict[str, Any]]: 모델 정보 목록
        """
        return [
            {
                "model_id": "eleven_turbo_v2_5",
                "name": "Eleven Turbo v2.5",
                "description": "빠르고 고품질 음성 합성",
                "languages": ["en", "ko", "ja", "zh"]
            },
            {
                "model_id": "eleven_flash_v2_5",
                "name": "Eleven Flash v2.5",
                "description": "초고속 음성 합성",
                "languages": ["en", "ko", "ja", "zh"]
            },
            {
                "model_id": "eleven_multilingual_v2",
                "name": "Eleven Multilingual v2",
                "description": "다국어 음성 합성",
                "languages": ["en", "ko", "ja", "zh", "es", "fr", "de"]
            }
        ]
    
    async def text_to_speech(
        self,
        text: str,
        voice_id: Optional[str] = None,
        model: str = "eleven_flash_v2_5",
        save_path: Optional[str] = None
    ) -> Optional[bytes]:
        """
        텍스트를 음성으로 변환 (비동기)
        
        Args:
            text: 변환할 텍스트
            voice_id: 사용할 음성 ID (None이면 기본값 사용)
            model: 사용할 모델
            save_path: 저장할 파일 경로 (선택사항)
            
        Returns:
            Optional[bytes]: 생성된 오디오 데이터
        """
        self._check_client()
        
        if not text.strip():
            raise ValueError("변환할 텍스트가 비어있습니다")
        
        current_voice = voice_id or self.voice_id
        logger.info(f"TTS 처리 시작: 음성={current_voice}, 모델={model}")
        
        try:
            loop = asyncio.get_event_loop()
            audio_data = await loop.run_in_executor(
                None,
                self._text_to_speech_sync,
                text,
                current_voice,
                model,
                save_path
            )
            
            logger.info("TTS 처리 완료")
            return audio_data
            
        except Exception as e:
            logger.error(f"TTS 처리 실패: {e}")
            raise RuntimeError(f"음성 변환 실패: {e}")
    
    def _text_to_speech_sync(
        self,
        text: str,
        voice_id: str,
        model: str,
        save_path: Optional[str] = None
    ) -> bytes:
        """동기 TTS 처리 (내부 사용)"""
        try:
            # ElevenLabs API 호출
            audio = self.client.text_to_speech.convert(
                text=text,
                voice_id=voice_id,
                model_id=model,
                output_format="mp3_44100_128"
            )
            
            # 오디오 데이터를 바이트로 수집
            audio_bytes = b''.join(audio)
            
            # 파일 저장 (선택사항)
            if save_path:
                os.makedirs(os.path.dirname(save_path) or '.', exist_ok=True)
                with open(save_path, 'wb') as f:
                    f.write(audio_bytes)
                logger.info(f"오디오 파일 저장: {save_path}")
            
            return audio_bytes
            
        except Exception as e:
            logger.error(f"TTS 변환 중 오류: {e}")
            raise
    
    async def speak(
        self,
        text: str,
        voice_id: Optional[str] = None,
        model: str = "eleven_flash_v2_5"
    ) -> bool:
        """
        텍스트를 음성으로 변환하고 즉시 재생 (비동기)
        
        Args:
            text: 읽을 텍스트
            voice_id: 사용할 음성 ID
            model: 사용할 모델
            
        Returns:
            bool: 재생 성공 여부
        """
        self._check_client()
        
        try:
            current_voice = voice_id or self.voice_id
            logger.info(f"음성 재생 시작: {text[:30]}...")
            
            loop = asyncio.get_event_loop()
            await loop.run_in_executor(
                None,
                self._speak_sync,
                text,
                current_voice,
                model
            )
            
            logger.info("음성 재생 완료")
            return True
            
        except Exception as e:
            logger.error(f"음성 재생 실패: {e}")
            return False
    
    def _speak_sync(self, text: str, voice_id: str, model: str):
        """동기 음성 재생 (내부 사용)"""
        try:
            # 스트리밍 방식으로 즉시 재생
            audio_stream = self.client.text_to_speech.stream(
                text=text,
                voice_id=voice_id,
                model_id=model
            )
            stream(audio_stream)
        except Exception as e:
            logger.error(f"음성 재생 중 오류: {e}")
            raise
    
    def save_audio_temp(self, audio_data: bytes, suffix: str = '.mp3') -> str:
        """
        오디오 데이터를 임시 파일로 저장
        
        Args:
            audio_data: 오디오 바이트 데이터
            suffix: 파일 확장자
            
        Returns:
            str: 임시 파일 경로
        """
        try:
            temp_file = tempfile.NamedTemporaryFile(
                suffix=suffix,
                dir=settings.TEMP_AUDIO_DIR,
                delete=False
            )
            
            temp_file.write(audio_data)
            temp_file.close()
            
            logger.debug(f"임시 오디오 파일 저장: {temp_file.name}")
            return temp_file.name
            
        except Exception as e:
            logger.error(f"임시 파일 저장 실패: {e}")
            raise RuntimeError(f"오디오 저장 실패: {e}")
    
    def cleanup_temp_file(self, file_path: str):
        """임시 파일 삭제"""
        try:
            if os.path.exists(file_path):
                os.unlink(file_path)
                logger.debug(f"임시 파일 삭제: {file_path}")
        except Exception as e:
            logger.warning(f"임시 파일 삭제 실패: {e}")
    
    def get_service_info(self) -> Dict[str, Any]:
        """TTS 서비스 정보 반환"""
        return {
            "service": "ElevenLabs TTS",
            "default_voice_id": self.voice_id,
            "available_models": self.get_available_models(),
            "client_initialized": self.client is not None,
            "api_key_configured": settings.ELEVENLABS_API_KEY is not None
        } 