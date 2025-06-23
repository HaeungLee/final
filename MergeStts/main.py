#!/usr/bin/env python3
"""
MergeStts - FastAPI 기반 음성 서비스
메인 진입점

Usage:
    python main.py
    uvicorn main:app --host 0.0.0.0 --port 8082 --reload
"""

import os
import sys
import logging
from pathlib import Path

# 현재 디렉토리를 Python 경로에 추가
current_dir = Path(__file__).parent
sys.path.insert(0, str(current_dir))

from web_server import app
from config.settings import settings

# 로깅 설정
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

def main():
    """메인 실행 함수"""
    try:
        import uvicorn
        
        logger.info("🚀 MergeStts FastAPI 서버 시작")
        logger.info(f"📍 서버 주소: http://{settings.HOST}:{settings.PORT}")
        logger.info(f"📚 API 문서: http://{settings.HOST}:{settings.PORT}/docs")
        logger.info(f"📖 ReDoc: http://{settings.HOST}:{settings.PORT}/redoc")
        
        # FastAPI 서버 실행
        uvicorn.run(
            "main:app",
            host=settings.HOST,
            port=settings.PORT,
            reload=settings.DEBUG,
            log_level="info" if settings.DEBUG else "warning",
            access_log=settings.DEBUG
        )
        
    except KeyboardInterrupt:
        logger.info("🛑 서버 종료 (사용자 중단)")
    except Exception as e:
        logger.error(f"❌ 서버 시작 실패: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main() 