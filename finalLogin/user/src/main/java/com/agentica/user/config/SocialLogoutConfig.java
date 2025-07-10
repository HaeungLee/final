package com.agentica.user.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 소셜 로그아웃 URL 설정
 */
@Configuration
@Getter
public class SocialLogoutConfig {

    @Value("${spring.security.oauth2.client.registration.google.client-id:default-google-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.naver.client-id:default-naver-id}")
    private String naverClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id:default-kakao-id}")
    private String kakaoClientId;

    @Value("${server.port:8080}")
    private String serverPort;

    private static final String BASE_URL = "http://localhost";
    private static final String LOGOUT_REDIRECT_PATH = "/logout-complete";    /**
     * 구글 로그아웃 URL - 더 강력한 로그아웃 (세션 완전 종료)
     */
    public String getGoogleLogoutUrl() {
        String redirectUrl = BASE_URL + ":" + serverPort + LOGOUT_REDIRECT_PATH;
        // Google 계정에서 완전 로그아웃 - 모든 세션 종료 및 계정 선택 강제
        return "https://accounts.google.com/logout?continue=" + 
               URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8) +
               "&hl=ko&service=accountsettings";
    }    /**
     * 네이버 로그아웃 URL - Token Revocation과 함께 사용하는 브라우저 세션 로그아웃
     */
    public String getNaverLogoutUrl() {
        String redirectUrl = BASE_URL + ":" + serverPort + LOGOUT_REDIRECT_PATH;
        
        // 네이버 브라우저 세션 로그아웃 - Token Revocation과 함께 사용
        // 이 URL은 브라우저에서 네이버 세션만 만료시킴 (access_token 폐기와는 무관)
        // 보안을 위해 Token Revocation(/unlink)과 함께 사용하는 것을 권장
        return "https://nid.naver.com/nidlogin.logout?" +
               "returl=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8);
    }
    
    /**
     * 네이버 완전 로그아웃 URL - 모든 네이버 서비스에서 강제 로그아웃
     */
    public String getNaverCompleteLogoutUrl() {
        String redirectUrl = BASE_URL + ":" + serverPort + LOGOUT_REDIRECT_PATH;
        
        // 네이버 완전 로그아웃 - 모든 네이버 서비스 세션 삭제
        return "https://nid.naver.com/nidlogin.logout?" +
               "returl=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8) +
               "&service=all" +  // 모든 네이버 서비스에서 로그아웃
               "&mode=logout_all" +  // 모든 세션 강제 삭제
               "&force=true" +  // 강제 로그아웃
               "&clear_session=1";  // 세션 완전 정리
    }
      /**
     * 네이버 연동 해제 URL (서버 사이드) - OAuth 2.0 토큰 삭제
     */
    public String getNaverUnlinkUrl() {
        return "https://nid.naver.com/oauth2.0/token";
    }
    
    /**
     * 네이버 사용자 정보 조회 URL - 토큰 유효성 확인용
     */
    public String getNaverUserInfoUrl() {
        return "https://openapi.naver.com/v1/nid/me";
    }

    /**
     * 카카오 로그아웃 URL
     * (단순히 redirect용 URL 반환 – 실제 logout API는 백엔드에서 호출)
     */
    public String getKakaoLogoutUrl() {
        return BASE_URL + ":" + serverPort + LOGOUT_REDIRECT_PATH;
    }

    /**
     * 제공자별 로그아웃 리디렉션 URL 반환
     */
    public String getLogoutUrl(String provider) {
        if (provider == null) return null;        return switch (provider.toLowerCase()) {
            case "google" -> getGoogleLogoutUrl();
            case "naver" -> getNaverCompleteLogoutUrl();  // 완전 로그아웃 사용
            case "kakao" -> getKakaoLogoutUrl();
            default -> null;
        };
    }

    /**
     * 네이버 강제 로그아웃 URL - 모든 세션 완전 삭제
     */
    public String getNaverForceLogoutUrl() {
        String redirectUrl = BASE_URL + ":" + serverPort + LOGOUT_REDIRECT_PATH;
        
        // 네이버 강제 로그아웃 - 모든 네이버 서비스 세션 완전 삭제
        return "https://nid.naver.com/nidlogin.logout?" +
               "returl=" + URLEncoder.encode(redirectUrl, StandardCharsets.UTF_8) +
               "&service=all" +
               "&mode=logout_all"; // 모든 세션 강제 삭제
    }
}
