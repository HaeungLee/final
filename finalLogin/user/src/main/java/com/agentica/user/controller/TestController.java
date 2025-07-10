package com.agentica.user.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class TestController {
    
    @GetMapping("/test-login")
    public String testLogin(Model model) {
        log.info("=== TEST-LOGIN 컨트롤러가 호출되었습니다! ===");
        
        model.addAttribute("message", "테스트 메시지입니다!");
        
        log.info("=== login.html 템플릿을 반환합니다 (테스트) ===");
        return "login";
    }
    
    @GetMapping("/test-simple")
    public String testSimple() {
        log.info("=== TEST-SIMPLE 컨트롤러가 호출되었습니다! ===");
        return "main";
    }
}
