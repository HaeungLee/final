@echo off
echo ==============================================
echo  AI Voice Agent 개발 환경 실행
echo ==============================================
echo.
echo 실행 중인 서버:
echo - Port 8082: Python STT/TTS (MergeStts)
echo - Port 8083: Voice Proxy Server
echo - Port 3000: Node.js Agent Backend  
echo - Port 5173: React Client
echo.

echo 1. Python STT/TTS 서버 (8082) 확인 중...
start "Python STT/TTS" cmd /c "cd /d d:\final2\MergeStts && python web_server.py"
timeout /t 3 /nobreak > nul

echo 2. Voice Proxy 서버 (8083) 시작 중...
start "Voice Proxy" cmd /c "cd /d d:\final2\finalteam\test2agent\server && npm run voice-proxy"
timeout /t 3 /nobreak > nul

echo 3. Node.js Agent Backend (3000) 시작 중...
start "Node.js Backend" cmd /c "cd /d d:\final2\finalteam\test2agent\server && npm run dev"
timeout /t 3 /nobreak > nul

echo 4. React Client (5173) 시작 중...
start "React Client" cmd /c "cd /d d:\final2\finalteam\test2agent\client && npm run dev"

echo.
echo ==============================================
echo  모든 서버가 시작되었습니다!
echo ==============================================
echo.
echo 서버 상태 확인:
echo - Python STT/TTS: http://localhost:8082/health
echo - Voice Proxy: http://localhost:8083/health  
echo - Node.js Backend: http://localhost:3000
echo - React Client: http://localhost:5173
echo.
echo 종료하려면 각 터미널 창을 닫으세요.
pause
