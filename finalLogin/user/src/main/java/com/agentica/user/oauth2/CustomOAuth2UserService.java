package com.agentica.user.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.agentica.user.domain.member.Member;
import com.agentica.user.oauth2.CustomOAuth2User;
import com.agentica.user.dto.oauth2.OAuth2UserInfo;
import com.agentica.user.oauth2.provider.GoogleOAuth2UserInfo;
import com.agentica.user.oauth2.provider.KakaoOAuth2UserInfo;
import com.agentica.user.oauth2.provider.NaverOAuth2UserInfo;
import com.agentica.user.service.OAuth2MemberService;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final OAuth2MemberService oAuth2MemberService;@Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("=== OAuth2 사용자 로드 시작 ===");
        
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        
        log.info("OAuth2 Provider: {}", registrationId);
        log.info("OAuth2 사용자 속성: {}", oAuth2User.getAttributes());
        
        // 🔥 [1단계] 네이버 access_token 저장 (로그인 시점)
        if ("naver".equals(registrationId.toLowerCase())) {
            storeNaverAccessToken(userRequest);
        }
        
        OAuth2UserInfo userInfo = getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
        
        log.info("추출된 사용자 정보 - Provider: {}, Email: {}, Name: {}", 
                userInfo.getProvider(), userInfo.getEmail(), userInfo.getName());

        Member member = oAuth2MemberService.saveOrUpdateOAuth2User(userInfo);
        
        log.info("OAuth2 사용자 로드 완료 - Member ID: {}, Email: {}", member.getId(), member.getEmail());
        
        return new CustomOAuth2User(member, oAuth2User.getAttributes());
    }    /**
     * 🔥 [1단계 완성] 네이버 access_token을 세션에 저장
     * OAuth2UserRequest에서 직접 토큰 추출 (가장 확실한 방법)
     */
    private void storeNaverAccessToken(OAuth2UserRequest userRequest) {
        try {
            log.info("🔑 네이버 access_token 세션 저장 시작");
            
            // OAuth2UserRequest에서 직접 access_token 추출
            String accessToken = userRequest.getAccessToken().getTokenValue();
            
            if (accessToken != null && !accessToken.isEmpty()) {
                // 현재 HTTP 세션 획득 및 토큰 저장
                ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                HttpSession session = attr.getRequest().getSession(true);
                session.setAttribute("naverAccessToken", accessToken);
                
                log.info("✅ 네이버 access_token 세션 저장 완료 (길이: {})", 
                        accessToken.length());
                log.info("🔒 토큰 마스킹 값: {}***", 
                        accessToken.substring(0, Math.min(8, accessToken.length())));
                
            } else {
                log.error("❌ 네이버 access_token이 비어있음");
            }
            
        } catch (Exception e) {
            log.error("❌ 네이버 access_token 저장 실패: {}", e.getMessage(), e);
        }
    }private OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        log.info("OAuth2UserInfo 생성 시작 - Provider: {}", registrationId);
        
        try {
            OAuth2UserInfo userInfo = switch (registrationId.toLowerCase()) {
                case "google" -> new GoogleOAuth2UserInfo(attributes);
                case "naver" -> new NaverOAuth2UserInfo(attributes);
                case "kakao" -> new KakaoOAuth2UserInfo(attributes);
                default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
            };
            
            log.info("OAuth2UserInfo 생성 완료 - Provider: {}, ProviderId: {}, Email: {}", 
                    userInfo.getProvider(), userInfo.getProviderId(), userInfo.getEmail());
            
            return userInfo;
        } catch (Exception e) {
            log.error("OAuth2UserInfo 생성 실패 - Provider: {}, Error: {}", registrationId, e.getMessage(), e);
            throw e;
        }
    }
}
