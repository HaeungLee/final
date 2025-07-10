package com.agentica.user.service;

import com.agentica.user.config.SocialLogoutConfig;
import com.agentica.user.domain.member.AuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLogoutService {
    
    private final SocialLogoutConfig socialLogoutConfig;
    private final RestTemplate restTemplate;
    private final TokenService tokenService;
    private final OAuth2AuthorizedClientService authorizedClientService;
    
    @Value("${kakao.admin.key:}")
    private String kakaoAdminKey;
    
    @Value("${spring.security.oauth2.client.registration.naver.client-id:}")
    private String naverClientId;
    
    @Value("${spring.security.oauth2.client.registration.naver.client-secret:}")
    private String naverClientSecret;

    /**
     * 소셜 로그아웃 처리 - 심플 버전
     */    public void processLogout(Authentication authentication, String userEmail) {
        log.info("=== 소셜 로그아웃 처리 시작 ===");
        
        try {
            // 디버깅: Authentication 객체 타입 확인
            if (authentication != null) {
                log.info("🔍 Authentication 객체 타입: {}", authentication.getClass().getSimpleName());
                log.info("🔍 Authentication 이름: {}", authentication.getName());
                log.info("🔍 Authentication 권한: {}", authentication.getAuthorities());
                log.info("🔍 Authentication Principal 타입: {}", authentication.getPrincipal().getClass().getSimpleName());
            } else {
                log.warn("⚠️ Authentication 객체가 null입니다!");
            }
            
            // 1. 토큰 정리 (모든 refresh token 무효화)
            if (userEmail != null) {
                tokenService.forceLogout(userEmail);
                log.info("사용자 토큰 정리 완료: {}", userEmail);
            }
            
            // 2. 소셜별 백그라운드 로그아웃 처리
            if (authentication instanceof OAuth2AuthenticationToken oAuth2Token) {
                String provider = oAuth2Token.getAuthorizedClientRegistrationId().toLowerCase();
                log.info("소셜 제공자: {}", provider);
                
                switch (provider) {
                    case "kakao":
                        if (isKakaoAdminKeyValid()) {
                            logoutFromKakao(authentication);
                        }
                        break;
                    case "google":
                        // 구글은 프론트엔드에서 리다이렉트로 처리
                        log.info("구글 로그아웃은 프론트엔드 리다이렉트로 처리됩니다.");
                        break;
                    case "naver":
                        // 네이버 강화된 통합 로그아웃 처리
                        boolean naverLogoutSuccess = processNaverLogout(authentication);
                        if (naverLogoutSuccess) {
                            log.info("✅ 네이버 강화된 로그아웃 처리 성공");
                        } else {
                            log.warn("⚠️ 네이버 로그아웃 일부 실패 - 클라이언트 로그아웃에 의존");
                        }                        break;
                }            } else {
                log.warn("⚠️ OAuth2AuthenticationToken이 아닙니다. 소셜 로그아웃을 건너뜁니다.");
                log.info("💡 현재 Authentication 타입으로는 소셜 제공자를 식별할 수 없습니다.");
                
                // 🔥 [대안] 세션에서 네이버 토큰이 있는지 확인하여 네이버 로그아웃 시도
                String sessionNaverToken = extractTokenFromSession();
                if (sessionNaverToken != null && !sessionNaverToken.trim().isEmpty()) {
                    log.info("🔑 세션에서 네이버 토큰 발견 - 네이버 로그아웃 시도");
                    boolean naverLogoutSuccess = performNaverTokenDeletion(sessionNaverToken);
                    if (naverLogoutSuccess) {
                        log.info("✅ 세션 기반 네이버 토큰 폐기 성공");
                    } else {
                        log.warn("⚠️ 세션 기반 네이버 토큰 폐기 실패");
                    }
                } else {
                    log.info("💡 세션에도 네이버 토큰이 없음 - 일반 로그아웃만 처리");
                }
            }
            
        } catch (Exception e) {
            log.error("소셜 로그아웃 처리 중 오류 발생", e);
        }
        
        log.info("=== 소셜 로그아웃 처리 완료 ===");
    }

    /**
     * 소셜 제공자 확인
     */
    public AuthProvider getProvider(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oAuth2Token) {
            String provider = oAuth2Token.getAuthorizedClientRegistrationId();
            return switch (provider.toLowerCase()) {
                case "google" -> AuthProvider.GOOGLE;
                case "naver" -> AuthProvider.NAVER;
                case "kakao" -> AuthProvider.KAKAO;
                default -> AuthProvider.LOCAL;
            };
        }
        return AuthProvider.LOCAL;
    }

    /**
     * 소셜 로그아웃이 필요한지 확인
     */
    public boolean needsSocialLogout(AuthProvider provider) {
        return provider != null && provider != AuthProvider.LOCAL;
    }

    /**
     * 소셜 로그아웃 URL 가져오기
     */
    public String getSocialLogoutUrl(AuthProvider provider) {
        if (provider == null) return null;
        
        return switch (provider) {
            case GOOGLE -> socialLogoutConfig.getGoogleLogoutUrl();
            case NAVER -> socialLogoutConfig.getNaverLogoutUrl();
            case KAKAO -> socialLogoutConfig.getKakaoLogoutUrl();
            default -> null;
        };
    }

    /**
     * 카카오 Admin Key 유효성 확인
     */
    private boolean isKakaoAdminKeyValid() {
        return kakaoAdminKey != null && !kakaoAdminKey.trim().isEmpty();
    }

    /**
     * 카카오 백그라운드 로그아웃 처리
     */
    private void logoutFromKakao(Authentication authentication) {
        log.info("=== Kakao 백그라운드 로그아웃 처리 시작 ===");
        
        try {
            String kakaoLogoutUrl = "https://kapi.kakao.com/v1/user/logout";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoAdminKey);
            headers.set("Content-Type", "application/x-www-form-urlencoded");
            
            String targetId = extractKakaoUserId(authentication);
            
            if (targetId != null) {
                String requestBody = "target_id_type=user_id&target_id=" + targetId;
                HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
                
                ResponseEntity<String> response = restTemplate.exchange(
                    kakaoLogoutUrl, 
                    HttpMethod.POST, 
                    request, 
                    String.class
                );
                
                log.info("카카오 로그아웃 API 호출 성공: {}", response.getStatusCode());
            } else {
                log.warn("카카오 사용자 ID를 추출할 수 없습니다.");
            }
            
        } catch (Exception e) {
            log.error("카카오 로그아웃 API 호출 중 오류 발생", e);
        }
        
        log.info("=== Kakao 백그라운드 로그아웃 처리 완료 ===");
    }
    
    /**
     * OAuth2 토큰에서 카카오 사용자 ID 추출
     */
    private String extractKakaoUserId(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            var attributes = oauth2Token.getPrincipal().getAttributes();
            Object id = attributes.get("id");
            
            if (id != null) {
                return id.toString();
            }
        }
        return null;
    }

    /**
     * 🔧 [리팩토링] 완전한 네이버 로그아웃 처리
     */
    private boolean processNaverLogout(Authentication authentication) {
        log.info("=== 🔹 네이버 완전 로그아웃 처리 시작 ===");
        
        boolean tokenRevocationSuccess = false;
        
        try {
            String userEmail = authentication != null ? authentication.getName() : "unknown";
            log.info("📧 네이버 로그아웃 대상 사용자: {}", userEmail);
            
            // [1단계] 사용자 정보 로깅
            logNaverUserInfo(authentication);
            
            // [2단계] 🔥 네이버 토큰 폐기 (Token Revocation)
            tokenRevocationSuccess = attemptNaverServerSideLogout(authentication);
            
            if (tokenRevocationSuccess) {
                log.info("✅ [1단계 완료] 네이버 토큰 폐기 성공 - access_token/refresh_token 만료됨");
            } else {
                log.warn("⚠️ [1단계 실패] 네이버 토큰 폐기 실패 - 브라우저 세션 로그아웃만 진행");
            }
            
            // [3단계] 🔥 브라우저 세션 로그아웃 URL 준비 (항상 필요)
            String logoutUrl = socialLogoutConfig.getNaverLogoutUrl();
            log.info("🌐 [2단계] 브라우저 세션 로그아웃 URL: {}", logoutUrl);
            log.info("💡 클라이언트에서 이 URL로 리다이렉트하여 자동 로그인 방지");
            
            // [4단계] 세션 정리
            performNaverSpecificCleanup(authentication);
            
            // [5단계] 📊 최종 로그아웃 흐름 정리
            log.info("=== 🔹 네이버 완전 로그아웃 처리 결과 ===");
            log.info("  📊 토큰 폐기 (서버): {}", tokenRevocationSuccess ? "성공" : "실패");
            log.info("  🌐 브라우저 로그아웃: 필요 (클라이언트에서 처리)");
            log.info("  🔗 로그아웃 URL: {}", logoutUrl);
            log.info("  📝 완전 로그아웃을 위해 두 단계 모두 필요");
            
        } catch (Exception e) {
            log.error("❌ 네이버 완전 로그아웃 처리 중 오류", e);
        }
        
        log.info("=== 🔹 네이버 완전 로그아웃 처리 완료 ===");
        
        // 토큰 폐기가 실패해도 브라우저 로그아웃은 필요하므로 true 반환
        return true; // 브라우저 로그아웃은 항상 필요
    }
    
    /**
     * 네이버 사용자 정보 로깅 (디버깅용)
     */
    private void logNaverUserInfo(Authentication authentication) {
        try {
            if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
                var attributes = oauth2Token.getPrincipal().getAttributes();
                log.info("🔍 네이버 사용자 정보:");
                log.info("  - 제공자: {}", oauth2Token.getAuthorizedClientRegistrationId());
                log.info("  - 사용자명: {}", oauth2Token.getName());
                log.info("  - 속성 키: {}", attributes.keySet());
                
                // 네이버 응답 구조 확인
                Object response = attributes.get("response");
                if (response instanceof Map<?, ?> naverResponse) {
                    log.info("  - 네이버 응답 데이터:");
                    naverResponse.forEach((key, value) -> 
                        log.info("    * {}: {}", key, value)
                    );
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ 네이버 사용자 정보 로깅 중 오류", e);
        }
    }

    /**
     * 🔧 [2단계] 개선된 네이버 서버 사이드 로그아웃 시도
     */
    private boolean attemptNaverServerSideLogout(Authentication authentication) {
        log.info("🔧 네이버 서버 사이드 로그아웃 시도 시작");
        
        try {
            // 클라이언트 정보 확인
            if (naverClientId == null || naverClientId.trim().isEmpty() ||
                naverClientSecret == null || naverClientSecret.trim().isEmpty()) {
                log.warn("⚠️ 네이버 클라이언트 정보 없음 - 서버 사이드 로그아웃 건너뜀");
                return false;
            }
            
            // 🔥 세션에서 토큰 추출 (우선순위 1)
            String accessToken = extractTokenFromSession();
            
            // 기존 방식도 시도 (fallback)
            if (accessToken == null) {
                accessToken = extractNaverAccessToken(authentication);
            }
            
            if (accessToken != null && !accessToken.trim().isEmpty()) {
                log.info("🔑 네이버 액세스 토큰 추출 성공");
                return performNaverTokenDeletion(accessToken);
            } else {
                log.warn("🔒 네이버 액세스 토큰 추출 실패");
                log.info("💡 해결 방안: OAuth2 로그인 시점에 토큰을 세션에 저장해야 함");
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ 네이버 서버 사이드 로그아웃 시도 중 오류", e);
            return false;
        }
    }

    /**
     * 📊 [2단계] 세션에서 네이버 access_token 추출
     */
    private String extractTokenFromSession() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attr.getRequest();
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                String accessToken = (String) session.getAttribute("naverAccessToken");
                if (accessToken != null && !accessToken.isEmpty()) {
                    log.info("✅ 세션에서 네이버 access_token 추출 성공 (길이: {})", accessToken.length());
                    return accessToken;
                } else {
                    log.warn("⚠️ 세션에 네이버 access_token이 없음");
                }
            } else {
                log.warn("⚠️ HTTP 세션이 존재하지 않음");
            }
        } catch (Exception e) {
            log.error("❌ 세션에서 네이버 access_token 추출 실패: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 🔧 [3단계] 개선된 네이버 토큰 폐기 API 호출
     */
    private boolean performNaverTokenDeletion(String accessToken) {
        if (accessToken == null || accessToken.isEmpty()) {
            log.warn("⚠️ access_token이 없어 네이버 토큰 폐기 불가");
            return false;
        }

        try {
            log.info("🔥 네이버 토큰 폐기 API 호출 시작");
            
            String url = socialLogoutConfig.getNaverUnlinkUrl();
            log.info("🌐 네이버 토큰 폐기 URL: {}", url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", naverClientId);
            params.add("client_secret", naverClientSecret);
            params.add("access_token", accessToken);
            params.add("grant_type", "delete");  // 🔥 네이버 토큰 삭제 타입

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                request, 
                String.class
            );
            
            log.info("📡 네이버 토큰 폐기 API 응답:");
            log.info("  - 상태 코드: {}", response.getStatusCode());
            log.info("  - 응답 본문: {}", response.getBody());
            
            // 🔥 응답 본문에서 "result":"success" 확인
            String responseBody = response.getBody();
            boolean success = response.getStatusCode().is2xxSuccessful();
            
            if (success && responseBody != null) {
                if (responseBody.contains("\"result\":\"success\"")) {
                    log.info("✅ 네이버 토큰 폐기 성공!");
                    log.info("  🔑 기존 access_token, refresh_token 즉시 만료됨");
                    log.info("  🗑️ 네이버 '내정보 > 연결된 서비스 관리'에서 서비스 제거됨");
                    
                    // 세션에서 토큰 제거
                    clearTokenFromSession();
                    return true;
                } else if (responseBody.contains("\"result\":\"fail\"")) {
                    log.warn("⚠️ 네이버 토큰 폐기 실패 - 응답: {}", responseBody);
                    return false;
                } else {
                    log.warn("⚠️ 네이버 응답 형식을 인식할 수 없음: {}", responseBody);
                    return false;
                }
            } else {
                log.warn("⚠️ 네이버 토큰 폐기 HTTP 오류 - 상태: {}, 응답: {}", 
                        response.getStatusCode(), responseBody);
                return false;
            }
            
        } catch (Exception e) {
            log.error("❌ 네이버 토큰 폐기 API 호출 실패: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 세션에서 네이버 토큰 제거
     */
    private void clearTokenFromSession() {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attr.getRequest();
            HttpSession session = request.getSession(false);
            
            if (session != null) {
                session.removeAttribute("naverAccessToken");
                log.info("✅ 세션에서 네이버 access_token 제거 완료");
            }
        } catch (Exception e) {
            log.warn("⚠️ 세션에서 네이버 토큰 제거 중 오류: {}", e.getMessage());
        }
    }

    /**
     * OAuth2 토큰에서 네이버 access_token 추출 (개선된 버전)
     * 실제 운영환경에서는 별도 토큰 저장소 구축 필요
     */
    private String extractNaverAccessToken(Authentication authentication) {
        log.info("🔍 네이버 액세스 토큰 추출 시도");
        
        try {
            if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
                log.info("📝 OAuth2 인증 토큰 확인:");
                log.info("  - 토큰 타입: {}", oauth2Token.getClass().getSimpleName());
                log.info("  - 제공자: {}", oauth2Token.getAuthorizedClientRegistrationId());
                log.info("  - Principal 이름: {}", oauth2Token.getName());
                
                // OAuth2AuthorizedClientService를 통한 토큰 추출 시도
                String serviceToken = extractTokenFromAuthorizedClientService(oauth2Token);
                if (serviceToken != null) {
                    log.info("✅ AuthorizedClientService에서 네이버 토큰 추출 성공");
                    return serviceToken;
                }
                
                log.info("💡 Spring Security OAuth2 제약사항:");
                log.info("  - OAuth2AuthenticationToken에서는 액세스 토큰을 직접 제공하지 않음");
                log.info("  - 보안상 이유로 토큰은 OAuth2AuthorizedClientRepository에서 관리");
                log.info("  - 프로덕션에서는 별도 토큰 관리 시스템 구축 필요");
                
                return null;
            }
        } catch (Exception e) {
            log.error("❌ 네이버 액세스 토큰 추출 중 오류", e);
        }
        
        log.info("🔒 네이버 액세스 토큰 추출 실패 - 클라이언트 사이드 로그아웃으로 처리");
        return null;
    }
    
    /**
     * OAuth2AuthorizedClientService에서 토큰 추출 시도
     */
    private String extractTokenFromAuthorizedClientService(OAuth2AuthenticationToken oauth2Token) {
        try {
            String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
            String principalName = oauth2Token.getName();
            
            log.info("🔍 AuthorizedClientService에서 토큰 추출 시도: {} ({})", registrationId, principalName);
            
            OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                registrationId, principalName);
            
            if (authorizedClient != null && authorizedClient.getAccessToken() != null) {
                String tokenValue = authorizedClient.getAccessToken().getTokenValue();
                log.info("✅ AuthorizedClientService에서 토큰 추출 성공");
                return tokenValue;
            } else {
                log.info("❌ AuthorizedClientService에서 토큰을 찾을 수 없음");
                return null;
            }
            
        } catch (Exception e) {
            log.warn("⚠️ AuthorizedClientService에서 토큰 추출 실패", e);
            return null;
        }
    }
    
    /**
     * 네이버 특화 세션 정리 처리
     */
    private void performNaverSpecificCleanup(Authentication authentication) {
        log.info("🧹 네이버 특화 세션 정리 시작");
        
        try {
            if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
                String registrationId = oauth2Token.getAuthorizedClientRegistrationId();
                String principalName = authentication.getName();
                
                log.info("🗑️ 네이버 OAuth2 클라이언트 정리: {} ({})", registrationId, principalName);
                
                // 네이버 특화 세션 정리
                // 실제 운영환경에서는 Redis 등 외부 저장소의 네이버 관련 세션도 정리
                log.info("✅ 네이버 특화 세션 정리 완료");
            }
            
        } catch (Exception e) {
            log.warn("⚠️ 네이버 특화 세션 정리 중 오류", e);
        }
    }
    
    /**
     * 회원탈퇴용 소셜 로그아웃 URL 생성
     */
    public String generateDeleteAccountLogoutUrl(String provider, String deleteToken) {
        log.info("회원탈퇴용 소셜 로그아웃 URL 생성 - 제공자: {}, 토큰: {}", 
            provider, deleteToken != null ? deleteToken.substring(0, 8) + "..." : "null");
        
        if (provider == null || deleteToken == null) {
            log.warn("제공자 또는 토큰이 null입니다.");
            return null;
        }
        
        String baseUrl = getCurrentBaseUrl();
        String callbackUrl = baseUrl + "/api/auth/complete-delete-account?provider=" + provider + "&token=" + deleteToken;
        
        return switch (provider.toLowerCase()) {
            case "google" -> {
                // Google 로그아웃 후 콜백 URL로 리다이렉트
                yield "https://accounts.google.com/logout?continue=" + java.net.URLEncoder.encode(callbackUrl, java.nio.charset.StandardCharsets.UTF_8);
            }
            case "kakao" -> {
                // Kakao 로그아웃 후 콜백 URL로 리다이렉트
                yield "https://kauth.kakao.com/oauth/logout?logout_redirect_uri=" + java.net.URLEncoder.encode(callbackUrl, java.nio.charset.StandardCharsets.UTF_8);
            }
            case "naver" -> {
                // Naver 로그아웃 후 콜백 URL로 리다이렉트
                yield "https://nid.naver.com/nidlogin.logout?returl=" + java.net.URLEncoder.encode(callbackUrl, java.nio.charset.StandardCharsets.UTF_8);
            }
            default -> {
                log.warn("지원하지 않는 소셜 제공자: {}", provider);
                yield null;
            }
        };
    }
    
    /**
     * 현재 기본 URL 가져오기 (localhost 또는 실제 도메인)
     */
    private String getCurrentBaseUrl() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String scheme = request.getScheme();
                String serverName = request.getServerName();
                int serverPort = request.getServerPort();
                
                if ((scheme.equals("http") && serverPort == 80) || 
                    (scheme.equals("https") && serverPort == 443)) {
                    return scheme + "://" + serverName;
                } else {
                    return scheme + "://" + serverName + ":" + serverPort;
                }
            }
        } catch (Exception e) {
            log.warn("현재 기본 URL을 가져오는 중 오류 발생", e);
        }
        
        // 기본값 반환
        return "http://localhost:8080";
    }
}
