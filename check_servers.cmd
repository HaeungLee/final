@echo off
echo ==============================================
echo  서버 상태 확인
echo ==============================================
echo.

echo 1. Python STT/TTS 서버 (8082) 확인 중...
curl -s -m 5 http://localhost:8082/health >nul 2>&1
if errorlevel 1 (
    echo ❌ Python STT/TTS 서버 (8082) - 연결 실패
) else (
    echo ✅ Python STT/TTS 서버 (8082) - 정상
)

echo.
echo 2. Voice Proxy 서버 (8083) 확인 중...
curl -s -m 5 http://localhost:8083/health >nul 2>&1
if errorlevel 1 (
    echo ❌ Voice Proxy 서버 (8083) - 연결 실패 ^(주요 문제^)
    echo.
    echo 해결 방법:
    echo - quick_voice_proxy.cmd 실행
    echo - 또는 run_all_dev.cmd로 모든 서버 실행
) else (
    echo ✅ Voice Proxy 서버 (8083) - 정상
)

echo.
echo 3. Node.js Backend 서버 (3000) 확인 중...
curl -s -m 5 http://localhost:3000 >nul 2>&1
if errorlevel 1 (
    echo ❌ Node.js Backend 서버 (3000) - 연결 실패
) else (
    echo ✅ Node.js Backend 서버 (3000) - 정상
)

echo.
echo 4. React Client (5173) 확인 중...
curl -s -m 5 http://localhost:5173 >nul 2>&1
if errorlevel 1 (
    echo ❌ React Client (5173) - 연결 실패
) else (
    echo ✅ React Client (5173) - 정상
)

echo.
echo ==============================================
echo  포트 사용 상태
echo ==============================================
netstat -an | findstr :808
netstat -an | findstr :300
netstat -an | findstr :517

echo.
pause
