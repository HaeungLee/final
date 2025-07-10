import os
import time
import asyncio
import numpy as np
import sounddevice as sd
import whisper
from datetime import datetime
from typing import Optional, Tuple
import tempfile
import logging
import base64
import wave
import io

from config.settings import settings

logger = logging.getLogger(__name__)

class STTService:
    """Whisper 기반 음성 인식 서비스"""
    
    def __init__(self):
        """STT 서비스 초기화"""
        self.model = None
        self.sample_rate = settings.SAMPLE_RATE
        self.channels = 1  # Mono audio
        self.device_id = None
        self._initialize_model()
        self._initialize_audio_device()
    
    def _initialize_model(self):
        """Whisper 모델 초기화"""
        try:
            logger.info(f"Whisper 모델 로딩 중: {settings.WHISPER_MODEL}")
            self.model = whisper.load_model(settings.WHISPER_MODEL)
            logger.info("Whisper 모델 로딩 완료")
        except Exception as e:
            logger.error(f"Whisper 모델 로딩 실패: {e}")
            raise RuntimeError(f"Whisper 모델 초기화 실패: {e}")
    
    def _initialize_audio_device(self):
        """오디오 장치 자동 탐지 및 초기화"""
        try:
            self.device_id = self._find_working_device()
            if self.device_id is None:
                logger.warning("작동하는 오디오 입력 장치를 찾을 수 없습니다")
        except Exception as e:
            logger.error(f"오디오 장치 초기화 실패: {e}")
    
    def _find_working_device(self) -> Optional[int]:
        """작동하는 오디오 입력 장치 탐지"""
        try:
            devices = sd.query_devices()
            logger.info("오디오 입력 장치 자동 탐지 중...")
            
            for i, device in enumerate(devices):
                if device['max_input_channels'] > 0:
                    try:
                        # 0.1초 테스트 녹음
                        test_audio = sd.rec(
                            int(0.1 * self.sample_rate),
                            samplerate=self.sample_rate,
                            channels=self.channels,
                            dtype='float32',
                            device=i
                        )
                        sd.wait()
                        
                        logger.info(f"작동하는 장치 발견: {device['name']} (ID: {i})")
                        return i
                        
                    except Exception:
                        continue
            
            logger.warning("작동하는 오디오 입력 장치를 찾을 수 없습니다")
            return None
            
        except Exception as e:
            logger.error(f"장치 탐지 중 오류: {e}")
            return None
    
    async def record_audio(self, duration: float) -> Optional[np.ndarray]:
        """
        마이크로 오디오 녹음 (비동기)
        
        Args:
            duration: 녹음 시간 (초)
            
        Returns:
            numpy.ndarray: 녹음된 오디오 데이터
        """
        if self.device_id is None:
            raise RuntimeError(
                "작동하는 오디오 입력 장치가 없습니다. "
                "Windows 마이크 권한을 확인하세요."
            )
        
        logger.info(f"오디오 녹음 시작: {duration}초, 장치 ID: {self.device_id}")
        
        try:
            # 블로킹 녹음 작업을 별도 스레드에서 실행 (타임아웃 추가)
            loop = asyncio.get_event_loop()
            
            # 녹음 시간 + 5초 여유시간으로 타임아웃 설정
            timeout = duration + 5.0
            logger.info(f"녹음 타임아웃 설정: {timeout}초")
            
            audio_data = await asyncio.wait_for(
                loop.run_in_executor(
                    None,
                    self._record_sync,
                    duration
                ),
                timeout=timeout
            )
            
            logger.info("오디오 녹음 완료")
            return audio_data
            
        except asyncio.TimeoutError:
            logger.error(f"녹음 타임아웃 발생: {duration + 5.0}초 초과")
            raise RuntimeError(f"녹음 타임아웃: 마이크 접근 권한을 확인하세요")
        except Exception as e:
            logger.error(f"녹음 중 오류: {e}")
            raise RuntimeError(f"녹음 실패: {e}")
    
    def _record_sync(self, duration: float) -> np.ndarray:
        """동기 녹음 (내부 사용)"""
        try:
            logger.info(f"sounddevice 녹음 시작: {duration}초")
            audio_data = sd.rec(
                int(duration * self.sample_rate),
                samplerate=self.sample_rate,
                channels=self.channels,
                dtype='float32',
                device=self.device_id
            )
            
            logger.info("sounddevice 대기 중...")
            sd.wait()  # 녹음 완료까지 대기
            logger.info("sounddevice 녹음 완료")
            
            return audio_data.flatten()
            
        except Exception as e:
            logger.error(f"sounddevice 녹음 중 오류: {e}")
            raise RuntimeError(f"녹음 실패: {e}")
    
    async def transcribe_audio(
        self, 
        audio_data: Optional[np.ndarray] = None, 
        audio_path: Optional[str] = None
    ) -> str:
        """
        오디오를 텍스트로 변환 (비동기)
        
        Args:
            audio_data: 오디오 데이터 (numpy array)
            audio_path: 오디오 파일 경로
            
        Returns:
            str: 변환된 텍스트
        """
        if audio_path and os.path.exists(audio_path):
            # 파일에서 로딩
            logger.info(f"파일에서 STT 처리: {audio_path}")
            loop = asyncio.get_event_loop()
            result = await loop.run_in_executor(
                None,
                self._transcribe_file,
                audio_path
            )
        elif audio_data is not None:
            # 오디오 데이터 직접 처리
            logger.info("오디오 데이터 STT 처리 중...")
            loop = asyncio.get_event_loop()
            result = await loop.run_in_executor(
                None,
                self._transcribe_data,
                audio_data
            )
        else:
            raise ValueError("audio_data 또는 audio_path 중 하나는 필수입니다")
        
        logger.info(f"STT 결과: {result[:50]}...")
        return result
    
    def _transcribe_file(self, audio_path: str) -> str:
        """파일 기반 STT (내부 사용)"""
        try:
            result = self.model.transcribe(audio_path, language='ko')
            return result["text"].strip()
        except Exception as e:
            logger.error(f"파일 STT 처리 실패: {e}")
            return ""
    
    def _transcribe_data(self, audio_data: np.ndarray) -> str:
        """데이터 기반 STT (내부 사용)"""
        try:
            # 오디오 데이터 품질 체크
            if len(audio_data) == 0:
                logger.warning("오디오 데이터가 비어있음")
                return ""
            
            # 오디오 레벨 체크 (너무 조용한지 확인)
            audio_level = np.max(np.abs(audio_data))
            logger.info(f"오디오 레벨: {audio_level:.4f}, 길이: {len(audio_data)} 샘플")
            
            if audio_level < 0.001:  # 매우 조용함
                logger.warning("오디오 레벨이 너무 낮음 - 마이크가 음소거되었거나 너무 조용함")
                return ""
            
            result = self.model.transcribe(
                audio_data.astype(np.float32), 
                language='ko'
            )
            text = result["text"].strip()
            
            # Whisper 신뢰도 체크 (있는 경우)
            if hasattr(result, 'segments') and result.segments:
                avg_confidence = sum(seg.get('avg_logprob', 0) for seg in result.segments) / len(result.segments)
                logger.info(f"STT 신뢰도: {avg_confidence:.3f}")
            
            return text
        except Exception as e:
            logger.error(f"데이터 STT 처리 실패: {e}")
            return ""
    
    async def record_and_transcribe(self, duration: float) -> Tuple[str, float]:
        """
        녹음 + STT 통합 처리 (비동기)
        
        Args:
            duration: 녹음 시간 (초)
            
        Returns:
            Tuple[str, float]: (변환된 텍스트, 처리 시간)
        """
        start_time = time.time()
        
        # 1. 오디오 녹음
        audio_data = await self.record_audio(duration)
        if audio_data is None:
            raise RuntimeError("오디오 녹음 실패")
        
        # 2. STT 처리
        text = await self.transcribe_audio(audio_data=audio_data)
        
        processing_time = time.time() - start_time
        return text, processing_time
    
    def save_audio_temp(self, audio_data: np.ndarray) -> str:
        """
        임시 파일로 오디오 저장
        
        Args:
            audio_data: 오디오 데이터
            
        Returns:
            str: 임시 파일 경로
        """
        try:
            temp_file = tempfile.NamedTemporaryFile(
                suffix='.wav',
                dir=settings.TEMP_AUDIO_DIR,
                delete=False
            )
            
            # WAV 파일로 저장
            import wave
            with wave.open(temp_file.name, 'wb') as wf:
                wf.setnchannels(self.channels)
                wf.setsampwidth(2)  # 16-bit
                wf.setframerate(self.sample_rate)
                wf.writeframes((audio_data * 32767).astype(np.int16).tobytes())
            
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
    
    def get_device_info(self) -> dict:
        """현재 오디오 장치 정보 반환"""
        try:
            devices = sd.query_devices()
            current_device = devices[self.device_id] if self.device_id is not None else None
            
            return {
                "current_device_id": self.device_id,
                "current_device_name": current_device['name'] if current_device else None,
                "available_devices": [
                    {
                        "id": i,
                        "name": device['name'],
                        "max_input_channels": device['max_input_channels']
                    }
                    for i, device in enumerate(devices)
                    if device['max_input_channels'] > 0
                ]
            }
        except Exception as e:
            logger.error(f"장치 정보 조회 실패: {e}")
            return {"error": str(e)}
    
    async def transcribe_base64(self, audio_base64: str, audio_format: str = "wav") -> Tuple[str, float]:
        """
        Base64 인코딩된 오디오 데이터를 텍스트로 변환 (모바일 지원)
        
        Args:
            audio_base64: Base64 인코딩된 오디오 데이터
            audio_format: 오디오 포맷 ('wav', 'mp3' 등)
            
        Returns:
            Tuple[str, float]: (변환된 텍스트, 오디오 길이)
        """
        try:
            logger.info("Base64 오디오 데이터 처리 시작")
            
            # Base64 디코딩
            binary_data = base64.b64decode(audio_base64)
            audio_duration = 0.0
            
            # 임시 파일 생성
            with tempfile.NamedTemporaryFile(
                suffix=f'.{audio_format}',
                dir=settings.TEMP_AUDIO_DIR,
                delete=False
            ) as temp_file:
                # 바이너리 데이터를 파일로 저장
                temp_file.write(binary_data)
                temp_file_path = temp_file.name
            
            logger.info(f"Base64 오디오를 임시 파일로 저장: {temp_file_path}")
            
            try:
                # 오디오 길이 확인 시도 (WAV 파일인 경우)
                if audio_format.lower() == 'wav':
                    with wave.open(temp_file_path, 'rb') as wf:
                        frames = wf.getnframes()
                        rate = wf.getframerate()
                        audio_duration = frames / float(rate)
                        logger.info(f"WAV 파일 길이: {audio_duration:.2f}초")
            except Exception as e:
                logger.warning(f"오디오 길이 확인 실패: {e}")
                
            # STT 처리
            text = await self.transcribe_audio(audio_path=temp_file_path)
            
            # 임시 파일 정리
            self.cleanup_temp_file(temp_file_path)
            
            return text, audio_duration
            
        except Exception as e:
            logger.error(f"Base64 오디오 처리 실패: {e}")
            raise RuntimeError(f"Base64 오디오 처리 실패: {e}")