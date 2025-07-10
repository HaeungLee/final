``/**
 * 네이버 소셜 로그아웃 처리 (개선된 직접 리다이렉트 방식)
 * iframe 방식을 제거하고 직접 리다이렉트로 변경
 */

class NaverLogoutHandler {
    constructor() {
        this.apiBaseUrl = '/api/auth';
        this.logoutInProgress = false;
    }

    /**
     * 개선된 네이버 소셜 로그아웃 처리
     */
    async performSocialLogout() {
        if (this.logoutInProgress) {
            console.log('🔄 로그아웃이 이미 진행 중입니다.');
            return;
        }

        try {
            this.logoutInProgress = true;
            console.log('🚀 네이버 소셜 로그아웃 시작 (직접 리다이렉트 방식)');

            // 1단계: 서버에 로그아웃 요청
            const response = await fetch(`${this.apiBaseUrl}/social-logout`, {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                }
            });

            if (!response.ok) {
                throw new Error(`로그아웃 요청 실패: ${response.status}`);
            }

            const result = await response.json();
            console.log('📡 서버 로그아웃 응답:', result);

            if (result.success && result.data) {
                await this.handleLogoutResponse(result.data);
            } else {
                throw new Error('서버 로그아웃 처리 실패');
            }

        } catch (error) {
            console.error('❌ 소셜 로그아웃 중 오류:', error);
            this.handleLogoutError(error);
        } finally {
            this.logoutInProgress = false;
        }
    }

    /**
     * 로그아웃 응답 처리
     */
    async handleLogoutResponse(data) {
        console.log('📊 로그아웃 응답 데이터:', data);

        // 로컬 스토리지 및 세션 정리
        if (data.clearStorage) {
            this.clearLocalStorage();
        }

        // 네이버 로그아웃 처리
        if (data.actualProvider === 'NAVER') {
            if (data.redirectLogout && data.logoutUrl) {
                console.log('🔗 네이버 로그아웃 URL로 리다이렉트:', data.logoutUrl);
                
                // 직접 리다이렉트 방식 (iframe 사용 안함)
                this.redirectToNaverLogout(data.logoutUrl);
            } else if (data.backgroundLogout) {
                console.log('✅ 네이버 백그라운드 로그아웃 완료');
                this.redirectToHome();
            }
        } else {
            // 다른 소셜 로그인 또는 일반 로그아웃
            this.redirectToHome();
        }
    }

    /**
     * 네이버 로그아웃 페이지로 직접 리다이렉트
     */
    redirectToNaverLogout(logoutUrl) {
        console.log('🌐 네이버 로그아웃 페이지로 이동 중...');
        
        // 현재 페이지를 네이버 로그아웃 URL로 직접 이동
        // 네이버에서 로그아웃 처리 후 returl 파라미터로 지정된 콜백 URL로 리다이렉트됨
        window.location.href = logoutUrl;
    }

    /**
     * 로컬 스토리지 및 세션 정리
     */
    clearLocalStorage() {
        console.log('🧹 로컬 스토리지 정리 중...');
        
        // JWT 토큰 및 사용자 정보 제거
        const keysToRemove = [
            'accessToken',
            'refreshToken',
            'userInfo',
            'authToken',
            'loginProvider',
            'naverAccessToken',
            'naverUserInfo'
        ];

        keysToRemove.forEach(key => {
            localStorage.removeItem(key);
            sessionStorage.removeItem(key);
        });

        console.log('✅ 로컬 스토리지 정리 완료');
    }

    /**
     * 홈페이지로 리다이렉트
     */
    redirectToHome() {
        console.log('🏠 홈페이지로 이동 중...');
        
        // 약간의 지연 후 홈페이지로 이동
        setTimeout(() => {
            window.location.href = '/';
        }, 1000);
    }

    /**
     * 로그아웃 오류 처리
     */
    handleLogoutError(error) {
        console.error('❌ 로그아웃 오류 처리:', error);
        
        // 오류 발생 시에도 로컬 정리는 수행
        this.clearLocalStorage();
        
        // 사용자에게 알림
        alert('로그아웃 중 오류가 발생했습니다. 페이지를 새로고침해주세요.');
        
        // 강제로 홈페이지 이동
        this.redirectToHome();
    }

    /**
     * 로그아웃 버튼 이벤트 리스너 설정
     */
    setupLogoutButton(buttonSelector = '#logout-btn') {
        const logoutBtn = document.querySelector(buttonSelector);
        
        if (logoutBtn) {
            logoutBtn.addEventListener('click', async (e) => {
                e.preventDefault();
                
                // 사용자 확인
                if (confirm('정말 로그아웃하시겠습니까?')) {
                    await this.performSocialLogout();
                }
            });
            
            console.log('✅ 로그아웃 버튼 이벤트 리스너 설정 완료');
        } else {
            console.warn('⚠️ 로그아웃 버튼을 찾을 수 없습니다:', buttonSelector);
        }
    }
}

/**
 * 전용 네이버 로그아웃 유틸리티 함수들
 */
class NaverLogoutUtils {
    
    /**
     * 네이버 로그아웃 상태 확인
     */
    static checkNaverLogoutStatus() {
        const urlParams = new URLSearchParams(window.location.search);
        const fromLogout = urlParams.get('from') === 'naver-logout';
        
        if (fromLogout) {
            console.log('✅ 네이버 로그아웃 완료 - 콜백 URL에서 돌아옴');
            
            // URL 파라미터 정리
            const newUrl = window.location.origin + window.location.pathname;
            window.history.replaceState({}, document.title, newUrl);
            
            // 로그아웃 완료 메시지
            this.showLogoutCompleteMessage();
        }
    }
    
    /**
     * 로그아웃 완료 메시지 표시
     */
    static showLogoutCompleteMessage() {
        // 토스트 메시지 또는 알림 표시
        const message = document.createElement('div');
        message.className = 'logout-complete-message';
        message.textContent = '✅ 네이버 로그아웃이 완료되었습니다.';
        message.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background: #4CAF50;
            color: white;
            padding: 15px 20px;
            border-radius: 5px;
            z-index: 10000;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        `;
        
        document.body.appendChild(message);
        
        // 3초 후 메시지 제거
        setTimeout(() => {
            if (message.parentNode) {
                message.parentNode.removeChild(message);
            }
        }, 3000);
    }
    
    /**
     * 네이버 로그인 상태 확인
     */
    static async checkNaverLoginStatus() {
        try {
            const response = await fetch('/api/auth/status', {
                credentials: 'include'
            });
            
            if (response.ok) {
                const result = await response.json();
                return result.data?.provider === 'NAVER';
            }
        } catch (error) {
            console.error('네이버 로그인 상태 확인 실패:', error);
        }
        
        return false;
    }
}

/**
 * DOM 로드 완료 시 초기화
 */
document.addEventListener('DOMContentLoaded', function() {
    console.log('🔧 네이버 로그아웃 핸들러 초기화');
    
    // 로그아웃 핸들러 초기화
    const logoutHandler = new NaverLogoutHandler();
    logoutHandler.setupLogoutButton('#logout-btn');
    
    // 네이버 로그아웃 콜백 확인
    NaverLogoutUtils.checkNaverLogoutStatus();
    
    // 전역 객체로 등록 (디버깅용)
    window.naverLogoutHandler = logoutHandler;
    window.naverLogoutUtils = NaverLogoutUtils;
});

/**
 * 사용 예제:
 * 
 * HTML:
 * <button id="logout-btn">로그아웃</button>
 * 
 * JavaScript:
 * // 자동으로 설정됨 (DOMContentLoaded 이벤트에서)
 * 
 * 또는 수동으로:
 * const logoutHandler = new NaverLogoutHandler();
 * logoutHandler.performSocialLogout();
 */

export { NaverLogoutHandler, NaverLogoutUtils };
