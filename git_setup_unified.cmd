@echo off
echo ==============================================
echo  Git Repository 통합 작업
echo ==============================================
echo.

echo 현재 디렉토리: %cd%
echo.

echo 1. 기존 서브프로젝트 .git 디렉토리 백업 중...
if exist finalLogin\.git (
    echo Backing up finalLogin/.git to finalLogin/.git.backup
    move finalLogin\.git finalLogin\.git.backup 2>nul
)

if exist finalteam\.git (
    echo Backing up finalteam/.git to finalteam/.git.backup  
    move finalteam\.git finalteam\.git.backup 2>nul
)

if exist MergeStts\.git (
    echo Backing up MergeStts/.git to MergeStts/.git.backup
    move MergeStts\.git MergeStts\.git.backup 2>nul
)

if exist googlerefesh\.git (
    echo Backing up googlerefesh/.git to googlerefesh/.git.backup
    move googlerefesh\.git googlerefesh\.git.backup 2>nul
)

echo.
echo 2. 새로운 통합 Git Repository 초기화...
if not exist .git (
    git init
    echo Git repository 초기화 완료
) else (
    echo Git repository가 이미 존재합니다.
)

echo.
echo 3. Git 사용자 정보 설정 (필요시 수정)...
git config user.name "AI Voice Assistant Team"
git config user.email "team@aivoiceassistant.com"

echo.
echo 4. 모든 파일 스테이징...
git add .

echo.
echo 5. 초기 커밋 생성...
git commit -m "Initial commit: AI Voice Assistant Platform

- 마이크로서비스 아키텍처 통합
- Spring Security 인증 서버 (finalLogin)
- Node.js Agent Backend (finalteam/test2agent)  
- Python STT/TTS 서버 (MergeStts)
- React Client & React Native
- Voice Proxy 서버
- 통합 실행 스크립트 및 문서"

echo.
echo ==============================================
echo  Git Repository 통합 완료!
echo ==============================================
echo.
echo 다음 단계:
echo 1. 원격 repository 연결:
echo    git remote add origin [your-repo-url]
echo.
echo 2. 첫 번째 푸시:
echo    git push -u origin main
echo.
echo 3. 백업된 .git.backup 폴더들은 필요시 삭제:
echo    rmdir /s finalLogin\.git.backup
echo    rmdir /s finalteam\.git.backup  
echo    rmdir /s MergeStts\.git.backup
echo.
pause
