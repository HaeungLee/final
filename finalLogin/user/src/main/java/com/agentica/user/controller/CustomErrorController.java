package com.agentica.user.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Controller
public class CustomErrorController implements ErrorController {
    
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        
        if (status != null) {
            Integer statusCode = Integer.valueOf(status.toString());
            
            if (statusCode == HttpStatus.NOT_FOUND.value()) {
                log.warn("404 에러 발생 - URI: {}", request.getRequestURI());
                model.addAttribute("errorTitle", "페이지를 찾을 수 없습니다");
                model.addAttribute("errorMessage", "요청하신 페이지가 존재하지 않습니다.");
                return "error/404";
            } else if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
                log.error("500 에러 발생 - URI: {}, Exception: {}", 
                    request.getRequestURI(), exception);
                
                // 회원탈퇴 관련 오류인지 확인
                if (exception != null && exception.toString().contains("UsernameNotFoundException")) {
                    log.info("삭제된 사용자 토큰으로 인한 오류 - 로그인 페이지로 리다이렉트");
                    return "redirect:/login?error=user_deleted";
                }
                
                model.addAttribute("errorTitle", "서버 오류가 발생했습니다");
                model.addAttribute("errorMessage", "잠시 후 다시 시도해주세요.");
                return "error/500";
            }
        }
        
        // 기본 에러 페이지
        model.addAttribute("errorTitle", "오류가 발생했습니다");
        model.addAttribute("errorMessage", "예상치 못한 오류가 발생했습니다.");
        return "error/default";
    }
}
