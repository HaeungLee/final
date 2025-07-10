package com.agentica.user.controller;

import com.agentica.user.domain.member.Member;
import com.agentica.user.service.MemberService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE) // 가장 높은 우선순위 설정
public class WebController {
    
    private final MemberService memberService;    // 메인 페이지
    @GetMapping({"/", "/main"})
    public String home(Model model, 
                      @RequestParam(value = "logout", required = false) String logout,
                      @RequestParam(value = "social", required = false) String social) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isLoggedIn = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());

        model.addAttribute("isLoggedIn", isLoggedIn);
        
        // 로그아웃 성공 메시지
        if ("true".equals(logout)) {
            if (social != null && !social.equals("unknown")) {
                model.addAttribute("logoutMessage", 
                    String.format("%s 소셜 로그아웃이 완료되었습니다.", 
                    social.toUpperCase()));
            } else {
                model.addAttribute("logoutMessage", "로그아웃이 완료되었습니다.");
            }
        }        if (isLoggedIn && auth != null) {
            try {
                Member member = memberService.findByEmail(auth.getName());
                model.addAttribute("member", member);
                model.addAttribute("canUpdate", member.canUpdate());
                model.addAttribute("isSocialUser", member.isSocialUser());
            } catch (Exception e) {
                log.warn("인증된 사용자의 정보 조회 실패: {}", e.getMessage());
                model.addAttribute("isLoggedIn", false);
            }
        }

        return "main"; // templates/main.html
    }
      @GetMapping("/logout")
public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    boolean isOAuth2 = auth instanceof OAuth2AuthenticationToken;

    // 로그아웃 처리
    if (auth != null) {
        new SecurityContextLogoutHandler().logout(request, response, auth);
    }

    // 소셜 로그인 사용자라면 완전한 소셜 로그아웃 처리 안내
    if (isOAuth2) {
        String html = """
            <html><body>
            <div style='text-align:center; padding:50px; font-family:Arial,sans-serif;'>
                <h3>🔄 로그아웃 처리 중...</h3>
                <p>완전한 로그아웃을 위해 소셜 로그아웃을 진행합니다.</p>
                <p><small>잠시만 기다려주세요...</small></p>
            </div>
            <script>
              // 새로운 방식: /api/auth/social-logout으로 리디렉션
              setTimeout(function() {
                window.location.href = '/api/auth/social-logout';
              }, 1000);
            </script>
            </body></html>
        """;
        return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
    }

    // 일반 로그인은 바로 리디렉션
    return ResponseEntity.status(302)
            .header("Location", "/main?logout=true")
            .build();
}


    // 커스텀 로그인 페이지 (소셜 로그인용)
    @GetMapping(value = {"/custom-login"}, produces = "text/html")
    public String customLogin(@RequestParam(value = "error", required = false) String error,
                              @RequestParam(value = "logout", required = false) String logout,
                              @RequestParam(value = "message", required = false) String message,
                              Model model,
                              Authentication authentication) {

        log.info("=== [CustomLogin] 커스텀 로그인 컨트롤러 호출됨 ===");
        log.info("요청 파라미터 - error: {}, logout: {}, message: {}", error, logout, message);

        // 이미 로그인된 사용자는 홈으로 리디렉션
        if (authentication != null && authentication.isAuthenticated() && 
            !"anonymousUser".equals(authentication.getName())) {
            return "redirect:/";
        }

        if (error != null) {
            model.addAttribute("error", "로그인에 실패했습니다.");
        }
        if (logout != null) {
            model.addAttribute("message", "로그아웃되었습니다.");
        }
        if (message != null) {
            model.addAttribute("message", message);
        }

        log.info("=== login.html 템플릿 반환 ===");
        return "login"; // templates/login.html
    }

    @GetMapping("/join")
    public String join() {
        return "join";
    }
      @GetMapping("/profile")
    public String profile(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
          try {
            Member member = memberService.findByEmail(authentication.getName());
            model.addAttribute("member", member);
            model.addAttribute("canUpdate", member.canUpdate());
            model.addAttribute("isSocialUser", member.isSocialUser());
        } catch (Exception e) {
            return "redirect:/login";
        }
          return "profile";
    }    // 소셜 로그아웃 완료 후 리디렉션 처리
    @GetMapping("/logout-complete")
    public String logoutComplete(HttpServletRequest request, Model model) {
        log.info("=== 소셜 로그아웃 완료 처리 ===");
        
        // HTTP Referer 헤더를 통해 어떤 소셜 서비스에서 왔는지 추측 (선택적)
        String referer = request.getHeader("Referer");
        String socialService = "unknown";
        
        if (referer != null) {
            if (referer.contains("naver.com") || referer.contains("nid.naver.com")) {
                socialService = "naver";
            } else if (referer.contains("google.com") || referer.contains("accounts.google.com")) {
                socialService = "google";
            } else if (referer.contains("kakao.com") || referer.contains("kauth.kakao.com")) {
                socialService = "kakao";
            }
        }
        
        log.info("소셜 로그아웃 완료 - 추정 서비스: {} (Referer: {})", socialService, referer);
        
        // 세션 완전 정리
        request.getSession().invalidate();
        
        // 로그아웃 완료 메시지와 함께 홈으로 리디렉션
        return "redirect:/?logout=true&social=" + socialService;
    }    // 통합 개선된 소셜 로그아웃 처리 페이지
    @GetMapping("/logout-social")
    public String logoutSocial(
            HttpServletRequest request,
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "socialLogoutUrl", required = false) String socialLogoutUrl,
            Model model) {
        log.info("=== 통합 개선된 소셜 로그아웃 처리 페이지 ===");
        log.info("Provider: {}, SocialLogoutUrl: {}", provider, socialLogoutUrl);
        
        // 현재 인증 정보에서 provider 추출 시도
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (provider == null && auth instanceof OAuth2AuthenticationToken oauth2Token) {
            provider = oauth2Token.getAuthorizedClientRegistrationId();
            log.info("인증 정보에서 추출된 Provider: {}", provider);
        }
        
        // 추가 쿠키 삭제 처리
        if (request.getCookies() != null) {
            log.info("현재 쿠키 상태:");
            for (Cookie cookie : request.getCookies()) {
                log.info("  - 쿠키: {} = {}", cookie.getName(), cookie.getValue());
            }
        }
        
        model.addAttribute("provider", provider != null ? provider : "unknown");
        model.addAttribute("socialLogoutUrl", socialLogoutUrl != null ? socialLogoutUrl : "");
        model.addAttribute("timestamp", System.currentTimeMillis()); // 캐시 방지
        
        return "logout-social";
    }
    
    // 회원탈퇴 완료 페이지
    @GetMapping("/delete-account-complete")
    public String deleteAccountComplete(
            @RequestParam(value = "provider", required = false) String provider,
            Model model) {
        log.info("=== 회원탈퇴 완료 처리 ===");
        log.info("Provider: {}", provider);
        
        model.addAttribute("provider", provider != null ? provider : "unknown");
        
        return "delete-account-complete";
    }

    // 네이버 OAuth2 콜백 처리 (직접 리다이렉트)
    @GetMapping("/login/oauth2/code/naver")
    public String handleNaverCallback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            HttpServletRequest request) {
        
        log.info("=== 네이버 OAuth2 콜백 직접 처리 ===");
        log.info("Code: {}, State: {}", code != null ? code.substring(0, 10) + "..." : "null", state);
        
        // 현재 인증 상태 확인
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            log.info("네이버 OAuth2 인증 성공 - 사용자: {}", auth.getName());
            
            // 사용자 정보 추출
            try {
                Member member = memberService.findByEmail(auth.getName());
                String redirectUrl = String.format("http://localhost:5173/?login=success&email=%s&name=%s", 
                                                  member.getEmail(), 
                                                  member.getName());
                
                log.info("네이버 로그인 성공 - React로 리다이렉트: {}", redirectUrl);
                return "redirect:" + redirectUrl;
                
            } catch (Exception e) {
                log.error("네이버 로그인 후 사용자 정보 조회 실패", e);
            }
        }
        
        // 인증 실패 시 로그인 페이지로
        log.warn("네이버 OAuth2 인증 실패 - 로그인 페이지로 리다이렉트");
        return "redirect:/custom-login?error=naver_login_failed";
    }

    // 🔧 네이버 콜백 디버깅용 임시 엔드포인트
    @GetMapping("/login/oauth2/code/{provider}")
    public String debugOAuth2Callback(
            @PathVariable String provider,
            @RequestParam Map<String, String> allParams,
            HttpServletRequest request) {
        
        log.info("=== 🔍 OAuth2 콜백 디버깅 ===");
        log.info("Provider: {}", provider);
        log.info("Request URL: {}", request.getRequestURL());
        log.info("Request URI: {}", request.getRequestURI());
        log.info("Query String: {}", request.getQueryString());
        log.info("All Parameters: {}", allParams);
        
        if ("naver".equals(provider)) {
            log.info("🟢 네이버 콜백이 Spring에 도달했습니다!");
            log.info("이제 Spring Security OAuth2가 처리해야 합니다.");
        }
        
        // Spring Security OAuth2가 처리하도록 리다이렉트
        return "redirect:/oauth2/authorization/" + provider;
    }
}
