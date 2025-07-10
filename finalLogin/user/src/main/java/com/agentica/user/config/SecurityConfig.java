package com.agentica.user.config;

import com.agentica.user.oauth2.CustomOAuth2UserService;
import com.agentica.user.oauth2.OAuth2SuccessHandler;
import com.agentica.user.security.SimpleLogoutSuccessHandler;
import com.agentica.user.security.JwtAuthenticationFilter;
import com.agentica.user.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Arrays;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final SimpleLogoutSuccessHandler simpleLogoutSuccessHandler;

    // ✅ 순환참조 해결: @Lazy를 사용한 생성자 주입
    public SecurityConfig(
        JwtTokenProvider jwtTokenProvider,
        CustomOAuth2UserService customOAuth2UserService,
        OAuth2SuccessHandler oAuth2SuccessHandler,
        @Lazy SimpleLogoutSuccessHandler simpleLogoutSuccessHandler
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        this.simpleLogoutSuccessHandler = simpleLogoutSuccessHandler;
    }

      @Bean
    @Order(1) // 가장 높은 우선순위로 설정
    public SecurityFilterChain filterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/", "/main", "/custom-login", "/join", "/api/auth/**", 
                               "/oauth2/**", "/login/oauth2/code/**", // OAuth2 콜백 경로 명시적 허용
                               "/logout-complete", "/logout-social", "/delete-account-complete").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .anyRequest().authenticated()
            )

            .formLogin(form -> form.disable())            .oauth2Login(oauth2 -> oauth2
                .loginPage("/custom-login") 
                .authorizationEndpoint(authorization -> 
                    authorization.authorizationRequestResolver(authorizationRequestResolver(clientRegistrationRepository))
                )
                .redirectionEndpoint(redirection -> 
                    redirection.baseUri("/login/oauth2/code/*") // 모든 OAuth2 콜백 경로 명시
                )
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler((request, response, exception) -> {
                    log.error("OAuth2 로그인 실패: {}", exception.getMessage());
                    response.sendRedirect("/custom-login?error=oauth2_failed");
                })
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    // 로그인 안 된 상태에서 보호된 리소스 접근 → 커스텀 로그인 페이지로 이동
                    response.sendRedirect("/custom-login");
                })            ).logout(logout -> logout
                .logoutUrl("/logout")
                .logoutRequestMatcher(request -> 
                    request.getServletPath().equals("/logout") && 
                    ("GET".equals(request.getMethod()) || "POST".equals(request.getMethod()))
                ) // GET/POST 요청 모두 허용
                .logoutSuccessHandler(simpleLogoutSuccessHandler)
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "refreshToken", "refresh_token", "accessToken", "access_token", 
                              "PGADMIN_LANGUAGE", "pgadmin_session", "jwt_token", "auth_token",
                              // 네이버 특화 쿠키 추가
                              "NID_AUT", "NID_JKL", "NID_SES")
                // 추가: 로그아웃 핸들러들을 체인으로 연결하여 확실한 쿠키 삭제 보장
                .addLogoutHandler((request, response, authentication) -> {
                    // JSESSIONID 강제 삭제 핸들러
                    forceDeleteJSessionId(request, response);
                })
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                             UsernamePasswordAuthenticationFilter.class);        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }    @Bean
    public OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clientRegistrationRepository) {
        return new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
    }    /**
     * OAuth2 로그인 시 항상 로그인 창을 표시하도록 설정 (자동 로그인 방지 강화)
     */
    @Bean
    public OAuth2AuthorizationRequestResolver authorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver authorizationRequestResolver =
                new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
        
        authorizationRequestResolver.setAuthorizationRequestCustomizer(customizer -> {
            // 모든 OAuth2 로그인에 강화된 자동 로그인 방지 파라미터 추가
            customizer.additionalParameters(params -> {
                // 기본적으로 모든 제공업체에 적용되는 파라미터
                params.put("prompt", "login consent"); // 로그인 + 권한 동의 강제
                
                // 추가 보안 파라미터 (일부 제공업체에서 지원)
                params.put("max_age", "0"); // 이전 인증 무효화
            });
        });
        
        return authorizationRequestResolver;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    
    /**
     * JSESSIONID 강제 삭제 핸들러 (SecurityConfig 레벨)
     */
    private void forceDeleteJSessionId(HttpServletRequest request, HttpServletResponse response) {
        log.info("🔥 SecurityConfig: JSESSIONID 강제 삭제 시작");
        
        try {
            // 1. 세션 상태 확인
            HttpSession session = request.getSession(false);
            if (session != null) {
                log.info("📍 로그아웃 시점 세션 확인: {}", session.getId());
            }
            
            // 2. 강력한 JSESSIONID 삭제 (다양한 조합)
            String[] paths = {"/", "/api", "/user", ""};
            boolean[] secureOptions = {false, true};
            boolean[] httpOnlyOptions = {true, false};
            
            for (String path : paths) {
                for (boolean secure : secureOptions) {
                    for (boolean httpOnly : httpOnlyOptions) {
                        Cookie cookie = new Cookie("JSESSIONID", "");
                        cookie.setPath(path);
                        cookie.setMaxAge(0);
                        cookie.setHttpOnly(httpOnly);
                        cookie.setSecure(secure);
                        response.addCookie(cookie);
                    }
                }
            }
            
            // 3. 직접 Set-Cookie 헤더 설정 (최종 보장)
            response.addHeader("Set-Cookie", "JSESSIONID=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
            response.addHeader("Set-Cookie", "JSESSIONID=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
            
            log.info("✅ SecurityConfig: JSESSIONID 강제 삭제 완료");
            
        } catch (Exception e) {
            log.error("❌ SecurityConfig: JSESSIONID 강제 삭제 중 오류", e);
        }
    }
}
