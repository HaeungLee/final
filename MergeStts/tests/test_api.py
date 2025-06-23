import pytest
import asyncio
from httpx import AsyncClient
from fastapi.testclient import TestClient

from web_server import app

# 동기 테스트 클라이언트
client = TestClient(app)

class TestHealthAPI:
    """헬스 체크 API 테스트"""
    
    def test_health_check(self):
        """헬스 체크 엔드포인트 테스트"""
        response = client.get("/api/health")
        assert response.status_code == 200
        
        data = response.json()
        assert "status" in data
        assert "service" in data
        assert "version" in data
        assert "timestamp" in data

class TestDurationPresetsAPI:
    """Duration 프리셋 API 테스트"""
    
    def test_duration_presets(self):
        """Duration 프리셋 엔드포인트 테스트"""
        response = client.get("/api/duration-presets")
        assert response.status_code == 200
        
        data = response.json()
        assert "presets" in data
        assert "recommended" in data
        assert "performance_info" in data
        
        # 프리셋 구조 확인
        presets = data["presets"]
        assert len(presets) == 3
        
        for preset in presets:
            assert "name" in preset
            assert "duration" in preset
            assert "description" in preset

class TestServiceInfoAPI:
    """서비스 정보 API 테스트"""
    
    def test_service_info(self):
        """서비스 정보 엔드포인트 테스트"""
        response = client.get("/api/service-info")
        assert response.status_code == 200
        
        data = response.json()
        assert "service_name" in data
        assert "version" in data
        assert "status" in data
        assert "settings" in data

class TestSTTAPI:
    """STT API 테스트 (실제 마이크 없이)"""
    
    def test_stt_request_validation(self):
        """STT 요청 검증 테스트"""
        # 유효한 요청
        valid_request = {"duration": 15.0}
        response = client.post("/api/record-and-transcribe", json=valid_request)
        # 실제 마이크가 없어도 요청 자체는 처리되어야 함
        assert response.status_code in [200, 503]  # 503은 서비스 초기화 실패
        
        # 잘못된 duration (너무 짧음)
        invalid_request = {"duration": 1.0}
        response = client.post("/api/record-and-transcribe", json=invalid_request)
        assert response.status_code == 422  # Validation error
        
        # 잘못된 duration (너무 김)
        invalid_request = {"duration": 60.0}
        response = client.post("/api/record-and-transcribe", json=invalid_request)
        assert response.status_code == 422  # Validation error

class TestTTSAPI:
    """TTS API 테스트"""
    
    def test_tts_request_validation(self):
        """TTS 요청 검증 테스트"""
        # 유효한 요청
        valid_request = {"text": "안녕하세요 테스트입니다"}
        response = client.post("/api/text-to-speech", json=valid_request)
        # API 키가 없어도 요청 구조는 검증되어야 함
        assert response.status_code in [200, 503]
        
        # 빈 텍스트
        invalid_request = {"text": ""}
        response = client.post("/api/text-to-speech", json=invalid_request)
        assert response.status_code == 422  # Validation error
        
        # 너무 긴 텍스트
        long_text = "a" * 1001
        invalid_request = {"text": long_text}
        response = client.post("/api/text-to-speech", json=invalid_request)
        assert response.status_code == 422  # Validation error

@pytest.mark.asyncio
class TestAsyncAPI:
    """비동기 API 테스트"""
    
    async def test_async_health_check(self):
        """비동기 헬스 체크 테스트"""
        async with AsyncClient(app=app, base_url="http://test") as ac:
            response = await ac.get("/api/health")
            assert response.status_code == 200
            
            data = response.json()
            assert data["service"] == "MergeStts Voice API"

if __name__ == "__main__":
    # 개별 테스트 실행
    pytest.main([__file__, "-v"]) 