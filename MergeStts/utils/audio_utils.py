import os
import tempfile
import logging
from typing import Optional
from datetime import datetime

logger = logging.getLogger(__name__)

def ensure_temp_dir(temp_dir: str) -> str:
    """
    임시 디렉토리 생성 및 확인
    
    Args:
        temp_dir: 임시 디렉토리 경로
        
    Returns:
        str: 생성된 디렉토리 경로
    """
    try:
        os.makedirs(temp_dir, exist_ok=True)
        return temp_dir
    except Exception as e:
        logger.error(f"임시 디렉토리 생성 실패: {e}")
        # 시스템 기본 임시 디렉토리 사용
        return tempfile.gettempdir()

def cleanup_old_temp_files(temp_dir: str, max_age_hours: int = 24):
    """
    오래된 임시 파일들 정리
    
    Args:
        temp_dir: 임시 디렉토리 경로
        max_age_hours: 최대 보관 시간 (시간)
    """
    if not os.path.exists(temp_dir):
        return
    
    try:
        import time
        current_time = time.time()
        max_age_seconds = max_age_hours * 3600
        
        cleaned_count = 0
        for filename in os.listdir(temp_dir):
            file_path = os.path.join(temp_dir, filename)
            
            if os.path.isfile(file_path):
                file_age = current_time - os.path.getmtime(file_path)
                
                if file_age > max_age_seconds:
                    try:
                        os.unlink(file_path)
                        cleaned_count += 1
                        logger.debug(f"오래된 임시 파일 삭제: {filename}")
                    except Exception as e:
                        logger.warning(f"파일 삭제 실패 {filename}: {e}")
        
        if cleaned_count > 0:
            logger.info(f"임시 파일 {cleaned_count}개 정리 완료")
            
    except Exception as e:
        logger.error(f"임시 파일 정리 중 오류: {e}")

def get_audio_file_info(file_path: str) -> Optional[dict]:
    """
    오디오 파일 정보 조회
    
    Args:
        file_path: 오디오 파일 경로
        
    Returns:
        Optional[dict]: 파일 정보 딕셔너리
    """
    if not os.path.exists(file_path):
        return None
    
    try:
        file_stats = os.stat(file_path)
        
        return {
            "file_path": file_path,
            "file_name": os.path.basename(file_path),
            "file_size": file_stats.st_size,
            "created_time": datetime.fromtimestamp(file_stats.st_ctime),
            "modified_time": datetime.fromtimestamp(file_stats.st_mtime),
            "extension": os.path.splitext(file_path)[1].lower()
        }
        
    except Exception as e:
        logger.error(f"파일 정보 조회 실패 {file_path}: {e}")
        return None

def generate_audio_filename(prefix: str = "audio", extension: str = ".wav") -> str:
    """
    타임스탬프 기반 오디오 파일명 생성
    
    Args:
        prefix: 파일명 접두사
        extension: 파일 확장자
        
    Returns:
        str: 생성된 파일명
    """
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S_%f")[:-3]  # 밀리초까지
    return f"{prefix}_{timestamp}{extension}"

def validate_audio_duration(duration: float, min_duration: float = 1.0, max_duration: float = 60.0) -> bool:
    """
    오디오 duration 값 검증
    
    Args:
        duration: 검증할 duration 값
        min_duration: 최소 허용 시간
        max_duration: 최대 허용 시간
        
    Returns:
        bool: 유효성 검증 결과
    """
    return min_duration <= duration <= max_duration

def format_processing_time(seconds: float) -> str:
    """
    처리 시간을 사용자 친화적 형식으로 포맷
    
    Args:
        seconds: 처리 시간 (초)
        
    Returns:
        str: 포맷된 시간 문자열
    """
    if seconds < 1:
        return f"{seconds*1000:.0f}ms"
    elif seconds < 60:
        return f"{seconds:.1f}초"
    else:
        minutes = int(seconds // 60)
        remaining_seconds = seconds % 60
        return f"{minutes}분 {remaining_seconds:.1f}초"

def sanitize_filename(filename: str) -> str:
    """
    파일명에서 특수문자 제거 및 안전한 형태로 변환
    
    Args:
        filename: 원본 파일명
        
    Returns:
        str: 안전한 파일명
    """
    import re
    
    # 특수문자를 언더스코어로 대체
    safe_name = re.sub(r'[<>:"/\\|?*]', '_', filename)
    
    # 연속된 언더스코어를 하나로 변환
    safe_name = re.sub(r'_+', '_', safe_name)
    
    # 앞뒤 공백 및 언더스코어 제거
    safe_name = safe_name.strip('_ ')
    
    # 빈 문자열이면 기본값 사용
    if not safe_name:
        safe_name = "audio_file"
    
    return safe_name 