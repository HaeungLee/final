@echo off
echo ==============================================
echo  Voice Proxy 서버 빠른 실행 (Port 8083)
echo ==============================================
echo.

cd /d "d:\final2\finalteam\test2agent\server"

echo 현재 디렉토리: %cd%
echo.

echo Voice Proxy 서버 시작 중...
call npm run voice-proxy

echo.
echo Voice Proxy 서버가 http://localhost:8083 에서 실행 중입니다.
echo.
echo 상태 확인: curl http://localhost:8083/health
echo 또는 브라우저에서: http://localhost:8083/health
echo.
echo 종료하려면 Ctrl+C를 누르세요.
pause
